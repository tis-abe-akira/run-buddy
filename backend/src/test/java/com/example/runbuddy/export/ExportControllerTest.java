package com.example.runbuddy.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.shared.Distance;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExportController.class)
@Import({InMemoryMonthlyPlanRepository.class, InMemoryRunResultRepository.class})
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonthlyPlanRepository plans;

    @Autowired
    private RunResultRepository runs;

    @Test
    void downloadsTheMonthAsCsv() throws Exception {
        plans.save(new MonthlyPlan(YearMonth.of(2026, 11), Distance.ofKilometers(150), List.of()));
        runs.saveAll(List.of(new RunResult("r1", LocalDateTime.of(2026, 11, 3, 7, 0),
                Distance.ofKilometers(10), Duration.ofMinutes(55), 150)));

        var result = mockMvc.perform(get("/api/export/2026-11.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("run-buddy-2026-11.csv")))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String[] lines = body.split("\r\n");
        assertThat(body).startsWith("﻿日付,曜日");
        assertThat(lines[3]).isEqualTo("2026-11-03,火,,,,1,10,55:00,5:30,150");
        assertThat(lines[lines.length - 1]).isEqualTo("合計,,,0,月間目標 150 km,1,10,55:00,5:30,");
    }

    @Test
    void malformedMonthIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/export/november.csv")).andExpect(status().isBadRequest());
    }
}
