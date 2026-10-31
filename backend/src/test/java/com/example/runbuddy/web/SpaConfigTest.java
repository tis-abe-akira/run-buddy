package com.example.runbuddy.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.health.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Uses the stand-in frontend in src/test/resources/static. */
@WebMvcTest(HealthController.class)
@Import(SpaConfig.class)
class SpaConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootServesTheFrontend() throws Exception {
        // Spring Boot's welcome page forwards / to index.html; MockMvc records the forward
        // instead of following it (a real server follows it, see the README check).
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void frontendRoutesServeIndexHtmlSoDeepLinksWork() throws Exception {
        mockMvc.perform(get("/plans/2026-11"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("run-buddy test index")));
    }

    @Test
    void existingAssetsAreServedAsThemselves() throws Exception {
        mockMvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("test asset")));
    }

    @Test
    void missingFilesAreNotFound() throws Exception {
        mockMvc.perform(get("/assets/missing.js")).andExpect(status().isNotFound());
    }

    @Test
    void unknownApiPathsAreNotFoundRatherThanHtml() throws Exception {
        mockMvc.perform(get("/api/nothing-here")).andExpect(status().isNotFound());
    }

    @Test
    void controllersStillWin() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
