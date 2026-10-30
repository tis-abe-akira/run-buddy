package com.example.runbuddy.plan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlanController.class)
@Import({InMemoryMonthlyPlanRepository.class, ApiExceptionHandler.class})
// The repository is shared across tests (the context is cached), so each test uses its own month.
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unplannedMonthReturnsAnEmptyPlan() throws Exception {
        mockMvc.perform(get("/api/plans/2026-01"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"targetKm":0.0,"workouts":[]}
                        """, true));
    }

    @Test
    void savedPlanCanBeReadBack() throws Exception {
        String plan = """
                {"targetKm":150,
                 "workouts":[{"date":"2026-11-08","type":"LONG","distanceKm":20,"note":"  LSD  "},
                             {"date":"2026-11-03","type":"EASY","distanceKm":6,"note":null}]}
                """;

        mockMvc.perform(put("/api/plans/2026-11").contentType(MediaType.APPLICATION_JSON).content(plan))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/plans/2026-11"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"targetKm":150.0,
                         "workouts":[{"date":"2026-11-03","type":"EASY","distanceKm":6.0,"note":""},
                                     {"date":"2026-11-08","type":"LONG","distanceKm":20.0,"note":"LSD"}]}
                        """, true));
    }

    @Test
    void workoutOutsideTheMonthIsABadRequest() throws Exception {
        String plan = """
                {"targetKm":100,"workouts":[{"date":"2026-12-01","type":"EASY","distanceKm":5}]}
                """;

        mockMvc.perform(put("/api/plans/2026-11").contentType(MediaType.APPLICATION_JSON).content(plan))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("workout on 2026-12-01 is outside the plan month 2026-11"));
    }

    @Test
    void workoutWithoutADateIsABadRequest() throws Exception {
        String plan = """
                {"targetKm":100,"workouts":[{"type":"EASY","distanceKm":5}]}
                """;

        mockMvc.perform(put("/api/plans/2026-11").contentType(MediaType.APPLICATION_JSON).content(plan))
                .andExpect(status().isBadRequest());
    }
}
