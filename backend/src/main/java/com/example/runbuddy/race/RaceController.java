package com.example.runbuddy.race;

import com.example.runbuddy.shared.Distance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Races the runner trains for (issue #16). Days left are counted from today's date. */
@RestController
@RequestMapping("/api/races")
public class RaceController {

    private final RaceRepository races;
    private final Clock clock;

    public RaceController(RaceRepository races, Clock clock) {
        this.races = races;
        this.clock = clock;
    }

    /** Every race, earliest first, past ones included (daysUntil is negative for those). */
    @GetMapping
    public List<RaceResponse> list() {
        LocalDate today = LocalDate.now(clock);
        return races.findAll().stream().map(race -> RaceResponse.from(race, today)).toList();
    }

    @PostMapping
    public ResponseEntity<RaceResponse> create(@RequestBody RaceBody body) {
        Race race = body.toRace(UUID.randomUUID());
        races.save(race);
        return ResponseEntity.status(HttpStatus.CREATED).body(RaceResponse.from(race, LocalDate.now(clock)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RaceResponse> update(@PathVariable UUID id, @RequestBody RaceBody body) {
        if (races.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Race race = body.toRace(id);
        races.save(race);
        return ResponseEntity.ok(RaceResponse.from(race, LocalDate.now(clock)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return races.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** What the screen sends. Distances are in kilometers; the target time is in seconds. */
    public record RaceBody(
            String name,
            LocalDate date,
            double distanceKm,
            @Schema(types = {"integer", "null"}, description = "target finishing time in seconds; null to just finish")
            Long targetSeconds) {

        Race toRace(UUID id) {
            if (name == null || date == null) {
                throw new IllegalArgumentException("a race needs a name and a date");
            }
            return new Race(
                    id,
                    name,
                    date,
                    Distance.ofKilometers(distanceKm),
                    targetSeconds == null ? null : Duration.ofSeconds(targetSeconds));
        }
    }

    public record RaceResponse(
            UUID id,
            String name,
            LocalDate date,
            double distanceKm,
            @Schema(types = {"integer", "null"}) Long targetSeconds,
            @Schema(types = {"integer", "null"}, description = "pace that meets the target time, rounded down")
            Long targetPaceSecondsPerKm,
            @Schema(description = "days from today; 0 on race day, negative once it is over") long daysUntil) {

        static RaceResponse from(Race race, LocalDate today) {
            return new RaceResponse(
                    race.id(),
                    race.name(),
                    race.date(),
                    race.distance().toKilometers(),
                    race.targetTime() == null ? null : race.targetTime().toSeconds(),
                    race.targetPace().map(Duration::toSeconds).orElse(null),
                    race.daysFrom(today));
        }
    }
}
