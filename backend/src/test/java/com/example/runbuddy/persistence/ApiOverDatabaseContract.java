package com.example.runbuddy.persistence;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Test
    default void aRaceIsAddedListedAndDeleted() throws Exception {
        String body = mockMvc().perform(post("/api/races").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Autumn marathon","date":"2026-11-22","distanceKm":42.195,"targetSeconds":18000}
                        """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(body, "$.id");

        mockMvc().perform(get("/api/races"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].targetPaceSecondsPerKm").value(426));

        mockMvc().perform(delete("/api/races/{id}", id)).andExpect(status().isNoContent());
        mockMvc().perform(get("/api/races")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    default void repeatingTheLatestImportAddsNothingNew() throws Exception {
        mockMvc().perform(post("/api/runs/latest/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").isString())
                .andExpect(jsonPath("$.added").isNumber());
        // The next import starts the day before the latest run, so it may pick up a run from a day
        // the first one (starting on the 1st) did not look at.
        mockMvc().perform(post("/api/runs/latest/import")).andExpect(status().isOk());

        // From then on, with nothing new at the source, nothing is added.
        mockMvc().perform(post("/api/runs/latest/import"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added").value(0));
    }
}
