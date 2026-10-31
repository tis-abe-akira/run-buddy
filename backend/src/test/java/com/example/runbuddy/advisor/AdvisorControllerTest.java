package com.example.runbuddy.advisor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import com.example.runbuddy.web.ApiExceptionHandler;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdvisorController.class)
@Import({AdvisorDirectory.class, ApiExceptionHandler.class, AdvisorControllerTest.Advisors.class})
class AdvisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsProvidersWithTheirModels() throws Exception {
        mockMvc.perform(get("/api/advisor/providers"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":"limited","displayName":"Limited","models":["m"]},
                         {"id":"planner","displayName":"Planner","models":["m1","m2"]},
                         {"id":"broken","displayName":"Broken","models":["m"]}]
                        """, true));
    }

    @Test
    void returnsTheReplyAndTheProposedPlanInThePlanShape() throws Exception {
        mockMvc.perform(post("/api/advisor/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"planner","model":"m2","month":"2026-11",
                         "messages":[{"speaker":"USER","text":"プランを作って"}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"reply":"m2: プランを作って",
                         "proposedPlan":{"month":"2026-11","targetKm":100.0,
                           "workouts":[{"date":"2026-11-01","type":"EASY","distanceKm":5.0,"note":""}]}}
                        """, true));
    }

    @Test
    void unknownProviderIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/advisor/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"openai","model":"gpt","month":"2026-11","messages":[{"speaker":"USER","text":"hi"}]}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void conversationEndingWithTheAdvisorIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/advisor/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"planner","model":"m1","month":"2026-11",
                         "messages":[{"speaker":"USER","text":"hi"},{"speaker":"ADVISOR","text":"hello"}]}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void providerFailureIsABadGateway() throws Exception {
        mockMvc.perform(post("/api/advisor/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"broken","model":"m","month":"2026-11","messages":[{"speaker":"USER","text":"hi"}]}
                        """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("quota exceeded"));
    }

    @Test
    void rateLimitIsATooManyRequests() throws Exception {
        mockMvc.perform(post("/api/advisor/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"limited","model":"m","month":"2026-11","messages":[{"speaker":"USER","text":"hi"}]}
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Wait a minute")));
    }

    @TestConfiguration
    static class Advisors {

        @Bean
        TrainingAdvisor limited() {
            return new TrainingAdvisor() {
                @Override
                public AdvisorInfo info() {
                    return new AdvisorInfo("limited", "Limited", List.of("m"));
                }

                @Override
                public Advice consult(Consultation consultation, String model) {
                    throw new AdvisorUnavailableException(
                            AdvisorUnavailableException.Reason.RATE_LIMITED, "Limited rate limit or quota exceeded", null);
                }
            };
        }

        @Bean
        TrainingAdvisor planner() {
            return new TrainingAdvisor() {
                @Override
                public AdvisorInfo info() {
                    return new AdvisorInfo("planner", "Planner", List.of("m1", "m2"));
                }

                @Override
                public Advice consult(Consultation consultation, String model) {
                    YearMonth month = consultation.planMonth();
                    MonthlyPlan plan = new MonthlyPlan(month, Distance.ofKilometers(100), List.of(
                            new PlannedWorkout(month.atDay(1), WorkoutType.EASY, Distance.ofKilometers(5), "")));
                    return new Advice(model + ": " + consultation.latestQuestion(), plan);
                }
            };
        }

        @Bean
        TrainingAdvisor broken() {
            return new TrainingAdvisor() {
                @Override
                public AdvisorInfo info() {
                    return new AdvisorInfo("broken", "Broken", List.of("m"));
                }

                @Override
                public Advice consult(Consultation consultation, String model) {
                    throw new AdvisorUnavailableException("quota exceeded", null);
                }
            };
        }
    }
}
