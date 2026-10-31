package com.example.runbuddy.activity.garmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.example.runbuddy.activity.ActivitySourceUnavailableException;
import com.example.runbuddy.activity.ActivitySourceUnavailableException.Reason;
import com.example.runbuddy.activity.RunResult;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestClient;

/**
 * The adapter against a mock HTTP server. Responses follow what the real API returned on
 * 2026-10-31 (src/test/resources/garmin, with made-up values); no request leaves the JVM.
 */
class GarminActivitySourceTest {

    private static final String BASE = "https://garmin.test";
    private static final String TOKEN_URL = BASE + GarminTokens.TOKEN_PATH;
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-31T00:00:00Z"));
    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GarminActivitySource source = GarminActivitySource.connect(builder, "test-key", clock);

    @Test
    void convertsTheRunsOfARecordedResponse() {
        expectToken("token-1");
        expectSearch(0).andRespond(withSuccess(new ClassPathResource("garmin/activities-2026-10.json"), MediaType.APPLICATION_JSON));

        List<RunResult> runs = source.fetchRuns(OCTOBER);

        // The gym session and the 0 m manual entry are dropped.
        assertThat(runs).extracting(RunResult::activityId)
                .containsExactly("garmin-1000000001", "garmin-1000000002", "garmin-1000000003");
        RunResult first = runs.getFirst();
        assertThat(first.startedAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 6, 1, 42));
        assertThat(first.distance().meters()).isEqualTo(6026);
        assertThat(first.duration()).isEqualTo(Duration.ofMillis(2_791_179));
        assertThat(first.averageHeartRate()).isEqualTo(121);
        assertThat(runs.get(1).averageHeartRate()).isEqualTo(131);
        assertThat(runs.get(2).heartRate()).isEmpty();
        server.verify();
    }

    @Test
    void asksForTheMonthsLocalDatesOldestFirstWithTheToken() {
        server.expect(once(), requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "ApiKey test-key"))
                .andRespond(withSuccess("token-1\n", MediaType.TEXT_PLAIN));
        server.expect(once(), requestTo(Matchers.startsWith(BASE + GarminActivitySource.SEARCH_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1"))
                .andExpect(queryParam("startDate", "2026-10-01"))
                .andExpect(queryParam("endDate", "2026-10-31"))
                .andExpect(queryParam("sortOrder", "asc"))
                .andExpect(queryParam("start", "0"))
                .andExpect(queryParam("limit", String.valueOf(GarminActivitySource.PAGE_SIZE)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(source.fetchRuns(OCTOBER)).isEmpty();
        server.verify();
    }

    @Test
    void readsPagesUntilOneIsNotFull() {
        int pageSize = GarminActivitySource.PAGE_SIZE;
        expectToken("token-1");
        expectSearch(0).andRespond(json(runs(1, pageSize)));
        expectSearch(pageSize).andRespond(json(runs(pageSize + 1, 1)));

        assertThat(source.fetchRuns(OCTOBER)).hasSize(pageSize + 1);
        server.verify();
    }

    @Test
    void keepsOneCopyOfAnActivityThatAppearsOnTwoPages() {
        int pageSize = GarminActivitySource.PAGE_SIZE;
        expectToken("token-1");
        expectSearch(0).andRespond(json(runs(1, pageSize)));
        expectSearch(pageSize).andRespond(json(runs(pageSize, 2)));

        assertThat(source.fetchRuns(OCTOBER)).hasSize(pageSize + 1);
    }

    @Test
    void dropsActivitiesOutsideTheMonth() {
        expectToken("token-1");
        expectSearch(0).andRespond(json("[" + activity(1, "2026-09-30 23:59:59") + "," + activity(2, "2026-10-01 00:00:00") + "]"));

        assertThat(source.fetchRuns(OCTOBER)).extracting(RunResult::activityId).containsExactly("garmin-2");
    }

    @Test
    void reusesTheTokenUntilItIsAboutToExpire() {
        expectToken("token-1");
        expectSearch(0).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1")).andRespond(json("[]"));
        expectSearch(0).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1")).andRespond(json("[]"));
        expectToken("token-2");
        expectSearch(0).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-2")).andRespond(json("[]"));

        source.fetchRuns(OCTOBER);
        clock.advance(GarminTokens.LIFETIME.minusMinutes(1));
        source.fetchRuns(OCTOBER);
        clock.advance(Duration.ofMinutes(1));
        source.fetchRuns(OCTOBER);
        server.verify();
    }

    @Test
    void renewsTheTokenOnceWhenGarminNoLongerAcceptsIt() {
        expectToken("token-1");
        expectSearch(0).andRespond(json("[]"));
        expectSearch(0).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-1")).andRespond(withUnauthorizedRequest());
        expectToken("token-2");
        expectSearch(0).andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-2")).andRespond(json("[" + activity(1, "2026-10-05 07:00:00") + "]"));

        source.fetchRuns(OCTOBER);
        assertThat(source.fetchRuns(OCTOBER)).hasSize(1);
        server.verify();
    }

    @Test
    void aRejectedKeyIsReportedAsSuch() {
        server.expect(once(), requestTo(TOKEN_URL)).andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> source.fetchRuns(OCTOBER))
                .isInstanceOfSatisfying(ActivitySourceUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.CREDENTIALS_REJECTED))
                .hasMessageContaining("GARMIN_API_KEY");
        server.verify();
    }

    @Test
    void aFreshTokenThatIsRejectedTooMeansTheKeyIsRejected() {
        expectToken("token-1");
        expectSearch(0).andRespond(withUnauthorizedRequest());
        expectToken("token-2");
        expectSearch(0).andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> source.fetchRuns(OCTOBER))
                .isInstanceOfSatisfying(ActivitySourceUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.CREDENTIALS_REJECTED));
        server.verify();
    }

    @Test
    void aForbiddenSearchIsAKeyProblemToo() {
        expectToken("token-1");
        expectSearch(0).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> source.fetchRuns(OCTOBER))
                .isInstanceOfSatisfying(ActivitySourceUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.CREDENTIALS_REJECTED));
    }

    @Test
    void theRateLimitIsReportedWithGarminsRetryAfter() {
        expectToken("token-1");
        expectSearch(0).andRespond(withTooManyRequests(120));

        assertThatThrownBy(() -> source.fetchRuns(OCTOBER))
                .isInstanceOfSatisfying(ActivitySourceUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.RATE_LIMITED))
                .hasMessageContaining("Retry-After: 120");
    }

    @Test
    void aServerErrorIsAnOutage() {
        expectToken("token-1");
        expectSearch(0).andRespond(withServerError());

        assertThatThrownBy(() -> source.fetchRuns(OCTOBER))
                .isInstanceOfSatisfying(ActivitySourceUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.UNAVAILABLE))
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void aNetworkFailureIsAnOutage() {
        server.expect(once(), requestTo(TOKEN_URL)).andRespond(withException(new IOException("connection reset")));

        assertThatThrownBy(() -> source.fetchRuns(OCTOBER))
                .isInstanceOfSatisfying(ActivitySourceUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.UNAVAILABLE))
                .hasMessageContaining("connection reset");
    }

    @Test
    void anUnreadableResponseIsAnOutage() {
        expectToken("token-1");
        expectSearch(0).andRespond(json("{\"not\": \"a list\"}"));

        assertThatThrownBy(() -> source.fetchRuns(OCTOBER))
                .isInstanceOfSatisfying(ActivitySourceUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.UNAVAILABLE));
    }

    private void expectToken(String token) {
        server.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess(token, MediaType.TEXT_PLAIN));
    }

    private ResponseActions expectSearch(int start) {
        return server.expect(once(), requestTo(Matchers.startsWith(BASE + GarminActivitySource.SEARCH_PATH)))
                .andExpect(queryParam("start", String.valueOf(start)));
    }

    private static org.springframework.test.web.client.ResponseCreator json(String body) {
        return withSuccess(body, MediaType.APPLICATION_JSON);
    }

    /** count runs with ids from firstId, one per minute from 2026-10-02 06:00. */
    private static String runs(int firstId, int count) {
        return IntStream.range(firstId, firstId + count)
                .mapToObj(id -> activity(id, "2026-10-02 06:%02d:%02d".formatted(id / 60, id % 60)))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String activity(long id, String startTimeLocal) {
        return """
                {"activityId": %d, "startTimeLocal": "%s",
                 "activityType": {"typeId": 1, "typeKey": "running", "parentTypeId": 17},
                 "distance": 5000.0, "duration": 1800.0, "averageHR": 140.0}"""
                .formatted(id, startTimeLocal);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }
}
