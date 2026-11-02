package com.example.runbuddy.race;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.runbuddy.shared.Distance;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RaceController.class)
@Import({InMemoryRaceRepository.class, RaceControllerTest.Today.class})
class RaceControllerTest {

    /** Today is 1 Nov 2026 in these tests. */
    @TestConfiguration
    static class Today {
        @Bean
        Clock clock() {
            ZoneId tokyo = ZoneId.of("Asia/Tokyo");
            return Clock.fixed(LocalDate.of(2026, 11, 1).atStartOfDay(tokyo).toInstant(), tokyo);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RaceRepository races;

    /** The application context, and so the in-memory repository, is shared between tests. */
    @BeforeEach
    void startWithoutRaces() {
        races.findAll().forEach(race -> races.delete(race.id()));
    }

    @Test
    void addsARaceAndShowsTheDaysLeftAndThePaceNeeded() throws Exception {
        mockMvc.perform(post("/api/races").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Autumn marathon", "date": "2026-11-22", "distanceKm": 42.195, "targetSeconds": 18000}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Autumn marathon"))
                .andExpect(jsonPath("$.distanceKm").value(42.195))
                .andExpect(jsonPath("$.targetSeconds").value(18000))
                .andExpect(jsonPath("$.targetPaceSecondsPerKm").value(426))
                .andExpect(jsonPath("$.daysUntil").value(21));

        assertThat(races.findAll()).singleElement()
                .satisfies(race -> assertThat(race.distance()).isEqualTo(Distance.ofMeters(42_195)));
    }

    @Test
    void aRaceToFinishHasNoTargetPace() throws Exception {
        mockMvc.perform(post("/api/races").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Spring half", "date": "2027-03-14", "distanceKm": 21.0975, "targetSeconds": null}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetSeconds").value(nullValue()))
                .andExpect(jsonPath("$.targetPaceSecondsPerKm").value(nullValue()));
    }

    @Test
    void listsEveryRaceEarliestFirstIncludingPastOnes() throws Exception {
        races.save(race("Spring half", LocalDate.of(2027, 3, 14)));
        races.save(race("Last year's 10k", LocalDate.of(2026, 10, 4)));

        mockMvc.perform(get("/api/races"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Last year's 10k"))
                .andExpect(jsonPath("$[0].daysUntil").value(-28))
                .andExpect(jsonPath("$[1].name").value("Spring half"));
    }

    @Test
    void editsARace() throws Exception {
        Race race = race("Autumn marathon", LocalDate.of(2026, 11, 22));
        races.save(race);

        mockMvc.perform(put("/api/races/{id}", race.id()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Autumn marathon", "date": "2026-11-22", "distanceKm": 42.195, "targetSeconds": 18000}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(race.id().toString()))
                .andExpect(jsonPath("$.targetSeconds").value(18000));

        assertThat(races.findById(race.id())).hasValueSatisfying(
                saved -> assertThat(saved.targetTime()).isEqualTo(Duration.ofHours(5)));
    }

    @Test
    void editingAnUnknownRaceIsNotFound() throws Exception {
        mockMvc.perform(put("/api/races/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Autumn marathon", "date": "2026-11-22", "distanceKm": 42.195, "targetSeconds": null}
                        """))
                .andExpect(status().isNotFound());
        assertThat(races.findAll()).isEmpty();
    }

    @Test
    void deletesARace() throws Exception {
        Race race = race("Autumn marathon", LocalDate.of(2026, 11, 22));
        races.save(race);

        mockMvc.perform(delete("/api/races/{id}", race.id())).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/races/{id}", race.id())).andExpect(status().isNotFound());
    }

    @Test
    void anInvalidRaceIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/races").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": " ", "date": "2026-11-22", "distanceKm": 42.195, "targetSeconds": null}
                        """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/races").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Autumn marathon", "date": null, "distanceKm": 42.195, "targetSeconds": null}
                        """))
                .andExpect(status().isBadRequest());
        assertThat(races.findAll()).isEmpty();
    }

    private static Race race(String name, LocalDate date) {
        return Race.plan(name, date, Distance.ofMeters(42_195), null);
    }
}
