package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LapTest {

    @Test
    void paceIsTimePerKilometer() {
        assertThat(lap(Distance.ofKilometers(1), Duration.ofSeconds(330)).pace()).contains(Duration.ofSeconds(330));
    }

    @Test
    void aLapWithoutDistanceHasNoPace() {
        assertThat(lap(Distance.ZERO, Duration.ofSeconds(30)).pace()).isEmpty();
    }

    @Test
    void lapsAreNumberedFromOne() {
        assertThatThrownBy(() -> new Lap(0, Duration.ZERO, Distance.ofKilometers(1), Duration.ofMinutes(5),
                null, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMaximumBelowItsAverageIsRejected() {
        assertThatThrownBy(() -> new Lap(1, Duration.ZERO, Distance.ofKilometers(1), Duration.ofMinutes(5),
                150, 149, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Lap(1, Duration.ZERO, Distance.ofKilometers(1), Duration.ofMinutes(5),
                null, null, 172, 170, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTrackPointMustBeOnEarth() {
        assertThatThrownBy(() -> new TrackPoint(Duration.ZERO, 91, 0, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrackPoint(Duration.ZERO, 0, 181, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Lap lap(Distance distance, Duration duration) {
        return new Lap(1, Duration.ZERO, distance, duration, null, null, null, null, null);
    }
}
