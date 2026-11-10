package com.example.runbuddy.review;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.activity.ActivityFileReader;
import com.example.runbuddy.activity.InMemoryRunFileRepository;
import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunDetail;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import com.example.runbuddy.shared.Distance;
import com.example.runbuddy.web.ApiExceptionHandler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RunReviewController.class)
@Import({RunReviewService.class, InMemoryRunResultRepository.class, InMemoryRunFileRepository.class,
        InMemoryRunReviewRepository.class, ApiExceptionHandler.class, RunReviewControllerTest.Ai.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RunReviewControllerTest {

    /** What the reviewer does next: answer, or fail with this exception. */
    @TestConfiguration
    static class Ai {
        @Bean
        AtomicReference<RuntimeException> failure() {
            return new AtomicReference<>();
        }

        @Bean
        RunReviewer reviewer(AtomicReference<RuntimeException> failure) {
            return new RunReviewer() {
                @Override
                public String id() {
                    return "gemini";
                }

                @Override
                public String model() {
                    return "flash";
                }

                @Override
                public Written review(RunReviewRequest request) {
                    if (failure.get() != null) {
                        throw failure.get();
                    }
                    return new Written("安定した10km", "前半から一定", "比べるランはありません");
                }

                @Override
                public String headline(RunReviewRequest request) {
                    return "安定した10km";
                }
            };
        }

        @Bean
        ActivityFileReader reader() {
            return zip -> new RunDetail(List.of(), List.of());
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-02T09:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RunResultRepository runs;

    @Autowired
    private AtomicReference<RuntimeException> failure;

    @Autowired
    private RunReviewService service;

    @BeforeEach
    void storeARun() {
        runs.saveAll(List.of(new RunResult("garmin-1", LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(10),
                Duration.ofMinutes(60), 150)));
    }

    @Test
    void aRunNotYetReviewedHasNoReview() throws Exception {
        mockMvc.perform(get("/api/runs/garmin-1/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review").value(nullValue()));
    }

    @Test
    void reviewingKeepsTheReviewForTheNextLook() throws Exception {
        mockMvc.perform(post("/api/runs/garmin-1/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("安定した10km"))
                .andExpect(jsonPath("$.runItself").value("前半から一定"))
                .andExpect(jsonPath("$.comparison").value("比べるランはありません"))
                .andExpect(jsonPath("$.brief").value(false))
                .andExpect(jsonPath("$.reviewer").value("gemini"))
                .andExpect(jsonPath("$.model").value("flash"))
                .andExpect(jsonPath("$.reviewedAt").value("2026-11-02T09:00:00Z"));

        mockMvc.perform(get("/api/runs/garmin-1/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.headline").value("安定した10km"));
    }

    /** The one-liner written on import (#36) has only the headline; asking for the review replaces it. */
    @Test
    void aBriefReviewIsShownAsBriefUntilTheDetailedOneIsAskedFor() throws Exception {
        service.reviewBriefly("garmin-1");

        mockMvc.perform(get("/api/runs/garmin-1/review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.headline").value("安定した10km"))
                .andExpect(jsonPath("$.review.brief").value(true))
                .andExpect(jsonPath("$.review.runItself").value(nullValue()))
                .andExpect(jsonPath("$.review.comparison").value(nullValue()));

        mockMvc.perform(post("/api/runs/garmin-1/review"))
                .andExpect(jsonPath("$.brief").value(false))
                .andExpect(jsonPath("$.runItself").value("前半から一定"));
    }

    @Test
    void anUnknownRunIsNotFound() throws Exception {
        mockMvc.perform(get("/api/runs/nope/review")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/runs/nope/review")).andExpect(status().isNotFound());
    }

    @Test
    void aFailingAiIsABadGatewayAndSavesNothing() throws Exception {
        failure.set(new AdvisorUnavailableException("gemini could not review the run", null));

        mockMvc.perform(post("/api/runs/garmin-1/review"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("gemini could not review the run"));
        mockMvc.perform(get("/api/runs/garmin-1/review")).andExpect(jsonPath("$.review").value(nullValue()));
    }

    @Test
    void aModelThatTakesTooLongIsAGatewayTimeout() throws Exception {
        failure.set(new AdvisorUnavailableException(AdvisorUnavailableException.Reason.TIMED_OUT, "gemini did not answer within 60 s", null));

        mockMvc.perform(post("/api/runs/garmin-1/review"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.detail").value("gemini did not answer within 60 s"));
    }

    @Test
    void aRateLimitIsTooManyRequests() throws Exception {
        failure.set(new AdvisorUnavailableException(AdvisorUnavailableException.Reason.RATE_LIMITED, "gemini rate limit or quota exceeded", null));

        mockMvc.perform(post("/api/runs/garmin-1/review")).andExpect(status().isTooManyRequests());
    }
}
