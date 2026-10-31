package com.example.runbuddy.day;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunMetrics;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DayController.class)
@Import({InMemoryMonthlyPlanRepository.class, InMemoryRunResultRepository.class})
class DayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonthlyPlanRepository plans;

    @Autowired
    private RunResultRepository runs;

    @Test
    void showsTheDaysPlanAndRunsWithTheirMeasurements() throws Exception {
        plans.save(new MonthlyPlan(YearMonth.of(2026, 11), Distance.ofKilometers(150), List.of(
                new PlannedWorkout(LocalDate.of(2026, 11, 8), WorkoutType.LONG, Distance.ofKilometers(18), "ゆっくり"))));
        runs.saveAll(List.of(
                new RunResult("evening", LocalDateTime.of(2026, 11, 8, 18, 40), Distance.ofKilometers(5),
                        Duration.ofMinutes(30), null),
                new RunResult("morning", LocalDateTime.of(2026, 11, 8, 6, 12), Distance.ofKilometers(16),
                        Duration.ofMinutes(100), 164, new RunMetrics(181, 172, 188, 98, 42))));

        mockMvc.perform(get("/api/days/2026-11-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-08"))
                .andExpect(jsonPath("$.plannedWorkouts[0].type").value("LONG"))
                .andExpect(jsonPath("$.plannedWorkouts[0].distanceKm").value(18.0))
                .andExpect(jsonPath("$.plannedWorkouts[0].note").value("ゆっくり"))
                .andExpect(jsonPath("$.runs.length()").value(2))
                .andExpect(jsonPath("$.runs[0].activityId").value("morning"))
                .andExpect(jsonPath("$.runs[0].paceSecondsPerKm").value(375))
                .andExpect(jsonPath("$.runs[0].averageHeartRate").value(164))
                .andExpect(jsonPath("$.runs[0].maxHeartRate").value(181))
                .andExpect(jsonPath("$.runs[0].averageCadence").value(172))
                .andExpect(jsonPath("$.runs[0].maxCadence").value(188))
                .andExpect(jsonPath("$.runs[0].averageStrideLengthCm").value(98))
                .andExpect(jsonPath("$.runs[0].elevationGainMeters").value(42))
                .andExpect(jsonPath("$.runs[1].activityId").value("evening"))
                .andExpect(jsonPath("$.runs[1].maxHeartRate").value(nullValue()));
    }

    @Test
    void aDayWithNothingIsEmptyNotMissing() throws Exception {
        mockMvc.perform(get("/api/days/2026-11-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedWorkouts.length()").value(0))
                .andExpect(jsonPath("$.runs.length()").value(0));
    }

    @Test
    void aMalformedDateIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/days/2026-13-01")).andExpect(status().isBadRequest());
    }
}
