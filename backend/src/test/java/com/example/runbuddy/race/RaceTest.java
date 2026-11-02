package com.example.runbuddy.race;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RaceTest {

    private static final LocalDate RACE_DAY = LocalDate.of(2026, 11, 22);
    private static final Distance MARATHON = Distance.ofMeters(42_195);

    @Test
    void aTargetTimeGivesThePaceNeeded() {
        Race race = Race.plan("Autumn marathon", RACE_DAY, MARATHON, Duration.ofHours(5));

        // 18,000 s / 42.195 km = 426.6 s/km. Rounded down, so running at the pace shown meets the
        // target: 7:06/km finishes in 4:59:43, while 7:07/km would take 5:00:08.
        assertThat(race.targetPace()).contains(Duration.ofSeconds(426));
    }

    @Test
    void withoutATargetTimeTheGoalIsToFinish() {
        Race race = Race.plan("Autumn marathon", RACE_DAY, MARATHON, null);

        assertThat(race.targetPace()).isEmpty();
    }

    @Test
    void daysUntilTheRaceCountFromToday() {
        Race race = Race.plan("Autumn marathon", RACE_DAY, MARATHON, null);

        assertThat(race.daysFrom(LocalDate.of(2026, 11, 1))).isEqualTo(21);
        assertThat(race.daysFrom(RACE_DAY)).isZero();
        assertThat(race.daysFrom(RACE_DAY.plusDays(1))).isEqualTo(-1);
    }

    @Test
    void aRaceIsUpcomingUntilTheDayItIsRun() {
        Race race = Race.plan("Autumn marathon", RACE_DAY, MARATHON, null);

        assertThat(race.isUpcoming(RACE_DAY.minusDays(1))).isTrue();
        assertThat(race.isUpcoming(RACE_DAY)).isTrue();
        assertThat(race.isUpcoming(RACE_DAY.plusDays(1))).isFalse();
    }

    @Test
    void theNameIsTrimmed() {
        assertThat(Race.plan("  Autumn marathon ", RACE_DAY, MARATHON, null).name()).isEqualTo("Autumn marathon");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void aRaceNeedsAName(String name) {
        assertThatThrownBy(() -> Race.plan(name, RACE_DAY, MARATHON, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNameIsAtMostOneHundredCharacters() {
        assertThat(Race.plan("a".repeat(100), RACE_DAY, MARATHON, null).name()).hasSize(100);
        assertThatThrownBy(() -> Race.plan("a".repeat(101), RACE_DAY, MARATHON, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRaceCoversSomeDistance() {
        assertThatThrownBy(() -> Race.plan("Autumn marathon", RACE_DAY, Distance.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTargetTimeMustBePositive() {
        assertThatThrownBy(() -> Race.plan("Autumn marathon", RACE_DAY, MARATHON, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eachPlannedRaceGetsItsOwnId() {
        assertThat(Race.plan("A", RACE_DAY, MARATHON, null).id()).isNotEqualTo(Race.plan("A", RACE_DAY, MARATHON, null).id());
    }
}
