package com.example.runbuddy.activity.garmin;

import com.example.runbuddy.activity.ActivityFileSource;
import com.example.runbuddy.activity.ActivitySource;
import com.example.runbuddy.activity.ActivitySourceUnavailableException.Reason;
import com.example.runbuddy.activity.ActivitySourceUnavailableException;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.shared.DateRange;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Runs from the Garmin Connect Personal API (ADR-0009): the activity search over a range of local
 * start dates, page by page, keeping only running activities.
 */
public class GarminActivitySource implements ActivitySource, ActivityFileSource {

    static final String SEARCH_PATH = "/activitylist-service/activities/search/activities";

    static final String DOWNLOAD_PATH = "/download-service/files/activity/{activityId}";

    /** The search's documented default page size; its maximum is not documented. */
    static final int PAGE_SIZE = 99;

    /** More pages than this means paging is not advancing (or the range is far too long); stop rather than loop. */
    static final int MAX_PAGES = 20;

    private static final Logger log = LoggerFactory.getLogger(GarminActivitySource.class);

    private static final ParameterizedTypeReference<List<GarminActivity>> ACTIVITIES = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;
    private final GarminTokens tokens;

    GarminActivitySource(RestClient client, GarminTokens tokens) {
        this.client = client;
        this.tokens = tokens;
    }

    /** The adapter for the real API, or a stub server in tests. */
    public static GarminActivitySource connect(String baseUrl, String apiKey, Clock clock) {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(10));
        requests.setReadTimeout(Duration.ofSeconds(30));
        return connect(RestClient.builder().requestFactory(requests).baseUrl(baseUrl), apiKey, clock);
    }

    static GarminActivitySource connect(RestClient.Builder builder, String apiKey, Clock clock) {
        RestClient client = builder.build();
        return new GarminActivitySource(client, new GarminTokens(client, apiKey, clock));
    }

    @Override
    public List<RunResult> fetchRuns(DateRange days) {
        return translatingErrors(() -> runsIn(days));
    }

    /**
     * The activity's original file, zipped (ADR-0012). Empty for a run that is not Garmin's, or one
     * Garmin no longer has (404). Manual entries have a file too, with one lap and no track.
     */
    @Override
    public Optional<byte[]> fetchFitZip(String activityId) {
        if (!activityId.startsWith(GarminActivity.ID_PREFIX)) {
            return Optional.empty();
        }
        long garminId;
        try {
            garminId = Long.parseLong(activityId.substring(GarminActivity.ID_PREFIX.length()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        return translatingErrors(() -> withToken(token -> download(garminId, token)));
    }

    private Optional<byte[]> download(long garminId, String token) {
        try {
            byte[] zip = client.get()
                    .uri(DOWNLOAD_PATH, garminId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(byte[].class);
            log.info("Garmin: downloaded the activity file of {} ({} bytes)", garminId, zip == null ? 0 : zip.length);
            return Optional.ofNullable(zip).filter(bytes -> bytes.length > 0);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Garmin: no activity file for {} (404)", garminId);
            return Optional.empty();
        }
    }

    /** Garmin's HTTP failures as the domain's {@link ActivitySourceUnavailableException}, with a reason. */
    private <T> T translatingErrors(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new ActivitySourceUnavailableException(Reason.CREDENTIALS_REJECTED,
                    "Garmin rejected the Personal API key (HTTP " + e.getStatusCode().value()
                            + "). It may have expired or been deleted; create a new one in Garmin Connect and set GARMIN_API_KEY",
                    e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            String retryAfter = Optional.ofNullable(e.getResponseHeaders())
                    .map(h -> h.getFirst(HttpHeaders.RETRY_AFTER))
                    .map(s -> " (Retry-After: " + s + ")")
                    .orElse("");
            throw new ActivitySourceUnavailableException(Reason.RATE_LIMITED,
                    "Garmin's rate limit was reached" + retryAfter, e);
        } catch (RestClientResponseException e) {
            throw new ActivitySourceUnavailableException(Reason.UNAVAILABLE,
                    "Garmin answered HTTP " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            throw new ActivitySourceUnavailableException(Reason.UNAVAILABLE,
                    "Could not reach Garmin: " + e.getMostSpecificCause().getMessage(), e);
        } catch (RestClientException | IllegalStateException e) {
            throw new ActivitySourceUnavailableException(Reason.UNAVAILABLE,
                    "Unexpected response from Garmin: " + e.getMessage(), e);
        }
    }

    private List<RunResult> runsIn(DateRange days) {
        // Keyed by id: if an activity is added while we page, offsets shift and one may come twice.
        Map<Long, GarminActivity> activities = new LinkedHashMap<>();
        for (int page = 0; ; page++) {
            if (page == MAX_PAGES) {
                throw new IllegalStateException("more than " + MAX_PAGES * PAGE_SIZE + " activities in " + days);
            }
            List<GarminActivity> found = search(days, page * PAGE_SIZE);
            log.debug("Garmin: {} page {} (start {}) returned {} activities", days, page + 1, page * PAGE_SIZE, found.size());
            found.stream()
                    .filter(a -> a.activityId() != null)
                    .forEach(a -> activities.putIfAbsent(a.activityId(), a));
            if (found.size() < PAGE_SIZE) {
                break;
            }
        }
        List<RunResult> runs = activities.values().stream()
                .map(GarminActivity::toRunResult)
                .flatMap(Optional::stream)
                // The search filters by local start date already; this guards the range's ends.
                .filter(run -> days.contains(run.date()))
                .toList();
        log.info("Garmin: {} activities in {}, {} of them runs with a distance and time (others skipped)",
                activities.size(), days, runs.size());
        return runs;
    }

    private List<GarminActivity> search(DateRange days, int start) {
        return withToken(token -> search(days, start, token));
    }

    /** Calls Garmin with the access token. On 401 the token is renewed once; a second 401 means the key itself is rejected. */
    private <T> T withToken(Function<String, T> call) {
        // Outside the try: a token exchange answered with 401 means the key is rejected; no retry.
        String token = tokens.current();
        try {
            return call.apply(token);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                throw e;
            }
            log.warn("Garmin: access token rejected (401); requesting a new one and retrying once");
            tokens.invalidate();
            return call.apply(tokens.current());
        }
    }

    private List<GarminActivity> search(DateRange days, int start, String token) {
        List<GarminActivity> page = client.get()
                .uri(uri -> uri.path(SEARCH_PATH)
                        .queryParam("startDate", days.first())
                        .queryParam("endDate", days.last())
                        // Oldest first, so activities recorded while we page land after our offset.
                        .queryParam("sortOrder", "asc")
                        .queryParam("start", start)
                        .queryParam("limit", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(ACTIVITIES);
        return page == null ? List.of() : page;
    }
}
