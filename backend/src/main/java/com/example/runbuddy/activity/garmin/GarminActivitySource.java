package com.example.runbuddy.activity.garmin;

import com.example.runbuddy.activity.ActivitySource;
import com.example.runbuddy.activity.ActivitySourceUnavailableException;
import com.example.runbuddy.activity.ActivitySourceUnavailableException.Reason;
import com.example.runbuddy.activity.RunResult;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Runs from the Garmin Connect Personal API (ADR-0009): the activity search, one month of local
 * start dates, page by page, keeping only running activities.
 */
public class GarminActivitySource implements ActivitySource {

    static final String SEARCH_PATH = "/activitylist-service/activities/search/activities";

    /** The search's documented default page size; its maximum is not documented. */
    static final int PAGE_SIZE = 99;

    /** A month with more activities than this means paging is not advancing; stop rather than loop. */
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
    public List<RunResult> fetchRuns(YearMonth month) {
        try {
            return runsIn(month);
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

    private List<RunResult> runsIn(YearMonth month) {
        // Keyed by id: if an activity is added while we page, offsets shift and one may come twice.
        Map<Long, GarminActivity> activities = new LinkedHashMap<>();
        for (int page = 0; ; page++) {
            if (page == MAX_PAGES) {
                throw new IllegalStateException("more than " + MAX_PAGES * PAGE_SIZE + " activities in " + month);
            }
            List<GarminActivity> found = search(month, page * PAGE_SIZE);
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
                // The search filters by local start date already; this guards the month boundary.
                .filter(run -> YearMonth.from(run.date()).equals(month))
                .toList();
        log.info("Garmin: {} activities in {}, {} of them runs with a distance and time",
                activities.size(), month, runs.size());
        return runs;
    }

    /** One page. On 401 the token is renewed once; a second 401 means the key itself is rejected. */
    private List<GarminActivity> search(YearMonth month, int start) {
        String token = tokens.current();
        try {
            return search(month, start, token);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                throw e;
            }
            tokens.invalidate();
            return search(month, start, tokens.current());
        }
    }

    private List<GarminActivity> search(YearMonth month, int start, String token) {
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        List<GarminActivity> page = client.get()
                .uri(uri -> uri.path(SEARCH_PATH)
                        .queryParam("startDate", first)
                        .queryParam("endDate", last)
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
