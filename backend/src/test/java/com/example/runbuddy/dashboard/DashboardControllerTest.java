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
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import({InMemoryMonthlyPlanRepository.class, InMemoryRunResultRepository.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonthlyPlanRepository plans;

    @Autowired
    private RunResultRepository runs;

    @Test
    void summarisesPlanAgainstRuns() throws Exception {
        YearMonth november = YearMonth.of(2026, 11);
        plans.save(new MonthlyPlan(november, Distance.ofKilometers(40), List.of()));
        runs.saveAll(List.of(new RunResult("r1", LocalDateTime.of(2026, 11, 3, 7, 0),
                Distance.ofKilometers(10), Duration.ofMinutes(55), 150)));

        mockMvc.perform(get("/api/dashboard/2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-11"))
                .andExpect(jsonPath("$.targetKm").value(40.0))
                .andExpect(jsonPath("$.actualKm").value(10.0))
                .andExpect(jsonPath("$.achievementRate").value(0.25))
                .andExpect(jsonPath("$.averagePaceSecondsPerKm").value(330))
                .andExpect(jsonPath("$.daily.length()").value(30))
                .andExpect(jsonPath("$.daily[2].cumulativeActualKm").value(10.0));
    }

    @Test
    void malformedMonthIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/dashboard/november")).andExpect(status().isBadRequest());
    }
}
