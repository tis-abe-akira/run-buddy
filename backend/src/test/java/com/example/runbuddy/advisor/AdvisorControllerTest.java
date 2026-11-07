package com.example.runbuddy.advisor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import com.example.runbuddy.web.ApiExceptionHandler;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Today is 15 Nov 2026 in these tests. */
@WebMvcTest(AdvisorController.class)
@Import({AdvisorDirectory.class, ApiExceptionHandler.class, InMemoryMonthlyPlanRepository.class,
        AdvisorControllerTest.Advisors.class})
class AdvisorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonthlyPlanRepository plans;

    @Test
    void listsProvidersWithTheirModels() throws Exception {
        mockMvc.perform(get("/api/advisor/providers"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":"reviser","displayName":"Reviser","models":["m"]},
                         {"id":"limited","displayName":"Limited","models":["m"]},
                         {"id":"planner","displayName":"Planner","models":["m1","m2"]},
                         {"id":"broken","displayName":"Broken","models":["m"]}]
                        """, true));
    }

    @Test
    void returnsTheReplyAndTheProposedPlanInThePlanShape() throws Exception {
        mockMvc.perform(post("/api/advisor/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"planner","model":"m2","month":"2026-12",
                         "messages":[{"speaker":"USER","text":"プランを作って"}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"reply":"m2: プランを作って",
                         "proposedPlan":{"month":"2026-12","targetKm":100.0,
                           "workouts":[{"date":"2026-12-01","type":"EASY","distanceKm":5.0,"note":""}]}}
                        """, true));
    }

    @Test
    void aMidMonthRevisionKeepsThePastAsPlannedAndOnlyChangesTodayOnwards() throws Exception {
        // Saved: 3rd (done), 20th. The advisor proposes the 1st (past), the 15th (today) and the 25th.
        plans.save(new MonthlyPlan(YearMonth.of(2026, 11), Distance.ofKilometers(150), List.of(
                new PlannedWorkout(LocalDate.of(2026, 11, 3), WorkoutType.EASY, Distance.ofKilometers(6), "done"),
                new PlannedWorkout(LocalDate.of(2026, 11, 20), WorkoutType.LONG, Distance.ofKilometers(15), ""))));

        mockMvc.perform(post("/api/advisor/consultations").contentType(MediaType.APPLICATION_JSON).content("""
                        {"provider":"reviser","model":"m","month":"2026-11",
                         "messages":[{"speaker":"USER","text":"見直して"}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proposedPlan.targetKm").value(120.0))
                .andExpect(jsonPath("$.proposedPlan.workouts[*].date").value(
                        org.hamcrest.Matchers.contains("2026-11-03", "2026-11-15", "2026-11-25")))
                .andExpect(jsonPath("$.proposedPlan.workouts[0].note").value("done"));
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
        Clock clock() {
            ZoneId tokyo = ZoneId.of("Asia/Tokyo");
            return Clock.fixed(LocalDate.of(2026, 11, 15).atStartOfDay(tokyo).toInstant(), tokyo);
        }

        /** Proposes a past day, today and a later day, as a model might when asked mid-month. */
        @Bean
        TrainingAdvisor reviser() {
            return new TrainingAdvisor() {
                @Override
                public AdvisorInfo info() {
                    return new AdvisorInfo("reviser", "Reviser", List.of("m"));
                }

                @Override
                public Advice consult(Consultation consultation, String model) {
                    YearMonth month = consultation.planMonth();
                    return new Advice("revised", new MonthlyPlan(month, Distance.ofKilometers(120), List.of(
                            new PlannedWorkout(month.atDay(1), WorkoutType.LONG, Distance.ofKilometers(30), ""),
                            new PlannedWorkout(month.atDay(15), WorkoutType.EASY, Distance.ofKilometers(8), ""),
                            new PlannedWorkout(month.atDay(25), WorkoutType.LONG, Distance.ofKilometers(20), ""))));
                }
            };
        }

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
