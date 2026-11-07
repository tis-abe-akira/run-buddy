package com.example.runbuddy.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.race.InMemoryRaceRepository;
import com.example.runbuddy.race.Race;
import com.example.runbuddy.race.RaceRepository;
import com.example.runbuddy.shared.Distance;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Today is 25 Nov 2026 in these tests. Each test gets fresh in-memory repositories. */
@WebMvcTest(NextStepsController.class)
@Import({TrainingSnapshots.class, InMemoryMonthlyPlanRepository.class, InMemoryRunResultRepository.class,
        InMemoryRaceRepository.class, NextStepsControllerTest.Today.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NextStepsControllerTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @TestConfiguration
    static class Today {
        @Bean
        Clock clock() {
            ZoneId tokyo = ZoneId.of("Asia/Tokyo");
            return Clock.fixed(LocalDate.of(2026, 11, 25).atStartOfDay(tokyo).toInstant(), tokyo);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonthlyPlanRepository plans;

    @Autowired
    private RunResultRepository runs;

    @Autowired
    private RaceRepository races;

    @Test
    void listsTheCardsMostPressingFirst() throws Exception {
        // 24 days have passed: 120 km of the 150 km target were due, 40 km were run, the last on the 10th.
        plans.save(new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of()));
        runs.saveAll(List.of(run("r1", 1, 30), run("r2", 10, 10)));

        mockMvc.perform(get("/api/next-steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards.length()").value(3))
                .andExpect(jsonPath("$.cards[0].kind").value("BEHIND_TARGET"))
                .andExpect(jsonPath("$.cards[0].month").value("2026-11"))
                .andExpect(jsonPath("$.cards[0].shortfallKm").value(80.0))
                .andExpect(jsonPath("$.cards[0].remainingKm").value(110.0))
                .andExpect(jsonPath("$.cards[0].daysLeft").value(6))
                .andExpect(jsonPath("$.cards[1].kind").value("NO_RUNS_LATELY"))
                .andExpect(jsonPath("$.cards[1].lastRunDate").value("2026-11-10"))
                .andExpect(jsonPath("$.cards[1].daysSince").value(15))
                .andExpect(jsonPath("$.cards[2].kind").value("MONTH_ENDING"))
                .andExpect(jsonPath("$.cards[2].nextMonth").value("2026-12"))
                .andExpect(jsonPath("$.cards[2].daysLeft").value(5));
    }

    @Test
    void anApproachingRaceTakesTheTopAndQuietsTheShortfall() throws Exception {
        plans.save(new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of()));
        plans.save(new MonthlyPlan(NOVEMBER.plusMonths(1), Distance.ofKilometers(100), List.of()));
        runs.saveAll(List.of(run("r1", 24, 10)));
        Race race = Race.plan("Autumn marathon", LocalDate.of(2026, 12, 6), Distance.ofKilometers(42.195), Duration.ofHours(5));
        races.save(race);

        mockMvc.perform(get("/api/next-steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards.length()").value(1))
                .andExpect(jsonPath("$.cards[0].kind").value("RACE_APPROACHING"))
                .andExpect(jsonPath("$.cards[0].raceId").value(race.id().toString()))
                .andExpect(jsonPath("$.cards[0].raceName").value("Autumn marathon"))
                .andExpect(jsonPath("$.cards[0].raceDate").value("2026-12-06"))
                .andExpect(jsonPath("$.cards[0].distanceKm").value(42.195))
                .andExpect(jsonPath("$.cards[0].daysToGo").value(11));
    }

    @Test
    void nothingToSayIsAnEmptyList() throws Exception {
        plans.save(new MonthlyPlan(NOVEMBER.plusMonths(1), Distance.ofKilometers(100), List.of()));
        runs.saveAll(List.of(run("r1", 24, 10)));

        mockMvc.perform(get("/api/next-steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards.length()").value(0));
    }

    private static RunResult run(String id, int day, double km) {
        return new RunResult(id, LocalDateTime.of(2026, 11, day, 6, 30), Distance.ofKilometers(km), Duration.ofMinutes(60), null);
    }
}
