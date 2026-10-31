package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RunResultTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 3, 6, 30);

    @Test
    void paceIsTimePerKilometer() {
        RunResult run = new RunResult("a", START, Distance.ofKilometers(10), Duration.ofMinutes(55), null);

        assertThat(run.pace()).isEqualTo(Duration.ofSeconds(330));
    }

    @Test
    void zeroDistanceIsRejected() {
        assertThatThrownBy(() -> new RunResult("a", START, Distance.ZERO, Duration.ofMinutes(1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroDurationIsRejected() {
        assertThatThrownBy(() -> new RunResult("a", START, Distance.ofKilometers(1), Duration.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRunWithoutMeasurementsHasNone() {
        RunResult run = new RunResult("a", START, Distance.ofKilometers(10), Duration.ofMinutes(55), 150);

        assertThat(run.metrics()).isEqualTo(RunMetrics.NONE);
    }

    @Test
    void maximumHeartRateBelowTheAverageIsRejected() {
        assertThatThrownBy(() -> new RunResult(
                        "a", START, Distance.ofKilometers(10), Duration.ofMinutes(55), 150,
                        new RunMetrics(149, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxHeartRate");
    }

    @Test
    void maximumHeartRateAloneIsAccepted() {
        RunMetrics metrics = new RunMetrics(180, null, null, null, null);

        assertThat(new RunResult("a", START, Distance.ofKilometers(10), Duration.ofMinutes(55), null, metrics).metrics())
                .isEqualTo(metrics);
    }
}
