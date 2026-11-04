package com.example.runbuddy.activity;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.activity.ActivitySourceUnavailableException.Reason;
import com.example.runbuddy.activity.fit.FitRunDetailReader;
import com.example.runbuddy.activity.fit.SyntheticFit;
import com.example.runbuddy.shared.Distance;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunDetailController.class)
@Import({RunDetailService.class, FitRunDetailReader.class, InMemoryRunResultRepository.class,
        InMemoryRunFileRepository.class, RunDetailControllerTest.Files.class})
class RunDetailControllerTest {

    /** What the source answers per activity id; a Reason makes it fail. */
    @TestConfiguration
    static class Files {
        @Bean
        Map<String, Object> answers() {
            return new ConcurrentHashMap<>();
        }

        @Bean
        ActivityFileSource activityFileSource(Map<String, Object> answers) {
            return activityId -> switch (answers.get(activityId)) {
                case byte[] zip -> Optional.of(zip);
                case Reason reason -> throw new ActivitySourceUnavailableException(reason, "source said: " + reason, null);
                case null, default -> Optional.empty();
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RunResultRepository runs;

    @Autowired
    private Map<String, Object> answers;

    @BeforeEach
    void storeRuns() {
        answers.clear();
        runs.saveAll(List.of(run("garmin-1"), run("garmin-2"), run("garmin-3"), run("garmin-4"), run("garmin-5"), run("dummy-2026-11-02")));
    }

    @Test
    void showsTheLapsAndHowManyTrackPointsThereAre() throws Exception {
        answers.put("garmin-1", new SyntheticFit()
                .record(0, 0.0001, 0.0001, 140)
                .record(7, 0.0002, 0.0001, 142)
                .lap(0, 1000f, 330f, 150, 162, 85, 0.5f, 92, 8)
                .lap(330, 0f, 20f, null, null, null, null, null, null)
                .zip());

        mockMvc.perform(get("/api/runs/garmin-1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.trackPoints").value(2))
                .andExpect(jsonPath("$.route.length()").value(2))
                .andExpect(jsonPath("$.route[1].latitude", number(0.0002)))
                .andExpect(jsonPath("$.route[1].longitude", number(0.0001)))
                .andExpect(jsonPath("$.route[0].lap").value(1))
                .andExpect(jsonPath("$.route[1].lap").value(1))
                .andExpect(jsonPath("$.laps.length()").value(2))
                .andExpect(jsonPath("$.laps[0].number").value(1))
                .andExpect(jsonPath("$.laps[0].distanceKm").value(1.0))
                .andExpect(jsonPath("$.laps[0].durationSeconds").value(330))
                .andExpect(jsonPath("$.laps[0].paceSecondsPerKm").value(330))
                .andExpect(jsonPath("$.laps[0].averageCadence").value(171))
                .andExpect(jsonPath("$.laps[0].elevationGainMeters").value(8))
                .andExpect(jsonPath("$.laps[1].startOffsetSeconds").value(330))
                .andExpect(jsonPath("$.laps[1].paceSecondsPerKm").value(nullValue()));
    }

    @Test
    void theRouteIsThinnedForDrawing() throws Exception {
        SyntheticFit fit = new SyntheticFit();
        for (int i = 0; i < 1000; i++) {
            fit.record(i * 7, 0.00001 * i, 0.0001, null);
        }
        fit.lap(0, 7000f, 7000f, null, null, null, null, null, null);
        answers.put("garmin-4", fit.zip());

        mockMvc.perform(get("/api/runs/garmin-4/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackPoints").value(1000))
                .andExpect(jsonPath("$.route.length()").value(300));
    }

    @Test
    void aRunWithoutPositionsHasAnEmptyRoute() throws Exception {
        answers.put("garmin-5", new SyntheticFit().lap(0, 1000f, 330f, null, null, null, null, null, null).zip());

        mockMvc.perform(get("/api/runs/garmin-5/detail"))
                .andExpect(jsonPath("$.route.length()").value(0));
    }

    @Test
    void aRunWithoutAnActivityFileIsNotAvailable() throws Exception {
        mockMvc.perform(get("/api/runs/dummy-2026-11-02/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.laps.length()").value(0))
                .andExpect(jsonPath("$.route.length()").value(0));
    }

    @Test
    void anUnknownRunIsNotFound() throws Exception {
        mockMvc.perform(get("/api/runs/garmin-404/detail")).andExpect(status().isNotFound());
    }

    @Test
    void aFailingSourceIsReportedWithItsReason() throws Exception {
        answers.put("garmin-2", Reason.RATE_LIMITED);

        mockMvc.perform(get("/api/runs/garmin-2/detail"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("RATE_LIMITED"));
    }

    @Test
    void anUnreadableFileIsABadGatewayWithItsReason() throws Exception {
        answers.put("garmin-3", "not a zip".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/runs/garmin-3/detail"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.reason").value("UNREADABLE_FILE"));
    }

    /** A JSON number within a microdegree of the expected one (FIT stores positions in semicircles). */
    private static Matcher<Number> number(double expected) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(Number actual) {
                return Math.abs(actual.doubleValue() - expected) < 1e-6;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("within 1e-6 of " + expected);
            }
        };
    }

    private static RunResult run(String id) {
        return new RunResult(id, LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(2), Duration.ofSeconds(650), 152);
    }
}
