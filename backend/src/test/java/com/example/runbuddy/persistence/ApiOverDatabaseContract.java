package com.example.runbuddy.persistence;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The HTTP API on top of a real database. Repository tests call the adapters directly; these go
 * through controllers and services, where a missing transaction would hide the current user from
 * row level security.
 */
public interface ApiOverDatabaseContract {

    MockMvc mockMvc();

    @Test
    default void savedPlanIsReturned() throws Exception {
        String plan = """
                {"targetKm":150,"workouts":[{"date":"2026-11-08","type":"LONG","distanceKm":20,"note":"LSD"}]}
                """;
        mockMvc().perform(put("/api/plans/2026-11").contentType(MediaType.APPLICATION_JSON).content(plan))
                .andExpect(status().isOk());

        mockMvc().perform(get("/api/plans/2026-11"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"targetKm":150.0,"workouts":[{"date":"2026-11-08","type":"LONG","distanceKm":20.0,"note":"LSD"}]}
                        """, true));
    }

    @Test
    default void importedRunsShowOnTheDashboard() throws Exception {
        mockMvc().perform(post("/api/runs/2026-09/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").isNumber());

        mockMvc().perform(get("/api/dashboard/2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runCount").value(org.hamcrest.Matchers.greaterThan(0)));
    }
}
