package com.example.runbuddy.race;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** What every {@link RaceRepository} must do. See MonthlyPlanRepositoryContract. */
public interface RaceRepositoryContract {

    RaceRepository repository();

    @Test
    default void aSavedRaceIsFoundById() {
        Race race = race("Autumn marathon", LocalDate.of(2026, 11, 22), Duration.ofHours(5));

        repository().save(race);

        assertThat(repository().findById(race.id())).contains(race);
    }

    @Test
    default void anUnknownIdIsAbsent() {
        assertThat(repository().findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    default void racesAreListedByDate() {
        Race spring = race("Spring half", LocalDate.of(2027, 3, 14), null);
        Race autumn = race("Autumn marathon", LocalDate.of(2026, 11, 22), Duration.ofHours(5));
        repository().save(spring);
        repository().save(autumn);

        assertThat(repository().findAll()).containsExactly(autumn, spring);
    }

    @Test
    default void savingAgainReplacesTheRace() {
        Race race = race("Autumn marathon", LocalDate.of(2026, 11, 22), null);
        Race edited = new Race(race.id(), "Autumn marathon (sub-5)", race.date(), race.distance(), Duration.ofHours(5));

        repository().save(race);
        repository().save(edited);

        assertThat(repository().findAll()).containsExactly(edited);
    }

    @Test
    default void aMissingTargetTimeStaysMissing() {
        Race race = race("Autumn marathon", LocalDate.of(2026, 11, 22), null);

        repository().save(race);

        assertThat(repository().findById(race.id())).hasValueSatisfying(found -> assertThat(found.targetTime()).isNull());
    }

    @Test
    default void deletingRemovesTheRaceAndSaysWhetherItWasThere() {
        Race race = race("Autumn marathon", LocalDate.of(2026, 11, 22), null);
        repository().save(race);

        assertThat(repository().delete(race.id())).isTrue();
        assertThat(repository().findById(race.id())).isEmpty();
        assertThat(repository().delete(race.id())).isFalse();
    }

    private static Race race(String name, LocalDate date, Duration targetTime) {
        return Race.plan(name, date, Distance.ofMeters(42_195), targetTime);
    }
}
