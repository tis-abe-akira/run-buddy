package com.example.runbuddy.activity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.activity.ActivitySourceUnavailableException.Reason;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** How a failing ActivitySource reaches the client: a problem detail whose "reason" says what to do. */
@WebMvcTest({RunController.class, LatestImportController.class, ActivitySourceController.class})
@Import({RunImportService.class, InMemoryRunResultRepository.class, RunControllerTest.FailingSource.class})
class RunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AtomicReference<Reason> failure;

    @BeforeEach
    void reset() {
        failure.set(null);
    }

    @Test
    void aRejectedKeyIsABadGatewayWithItsReason() throws Exception {
        failure.set(Reason.CREDENTIALS_REJECTED);

        mockMvc.perform(post("/api/runs/2026-10/import"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.reason").value("CREDENTIALS_REJECTED"))
                .andExpect(jsonPath("$.detail").value("source said: CREDENTIALS_REJECTED"));
    }

    @Test
    void theRateLimitIsTooManyRequests() throws Exception {
        failure.set(Reason.RATE_LIMITED);

        mockMvc.perform(post("/api/runs/2026-10/import"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("RATE_LIMITED"));
    }

    @Test
    void anOutageIsABadGateway() throws Exception {
        failure.set(Reason.UNAVAILABLE);

        mockMvc.perform(post("/api/runs/2026-10/import"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.reason").value("UNAVAILABLE"));
    }

    @Test
    void importsWhenTheSourceAnswers() throws Exception {
        mockMvc.perform(post("/api/runs/2026-10/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetched").value(0));
    }

    @Test
    void theLatestImportSaysWhichDaysItLookedAt() throws Exception {
        mockMvc.perform(post("/api/runs/latest/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-11-01"))
                .andExpect(jsonPath("$.to").value("2026-11-02"))
                .andExpect(jsonPath("$.fetched").value(0))
                .andExpect(jsonPath("$.added").value(0));
    }

    @Test
    void theLatestImportReportsAFailingSourceTheSameWay() throws Exception {
        failure.set(Reason.RATE_LIMITED);

        mockMvc.perform(post("/api/runs/latest/import"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.reason").value("RATE_LIMITED"));
    }

    @Test
    void tellsWhichSourceIsConfigured() throws Exception {
        mockMvc.perform(get("/api/activity-source"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("dummy"));
    }

    @TestConfiguration
    static class FailingSource {

        /** Today is 2 Nov 2026. */
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-02T03:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        AtomicReference<Reason> failure() {
            return new AtomicReference<>();
        }

        @Bean
        ActivitySource activitySource(AtomicReference<Reason> failure) {
            return month -> {
                Reason reason = failure.get();
                if (reason != null) {
                    throw new ActivitySourceUnavailableException(reason, "source said: " + reason, null);
                }
                return List.of();
            };
        }
    }
}
