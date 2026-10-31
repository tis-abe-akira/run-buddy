package com.example.runbuddy.activity.garmin;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunMetrics;
import com.example.runbuddy.activity.RunResult;
import org.junit.jupiter.api.Test;

/** Garmin's optional measurements: a value the domain would reject drops that value, never the run. */
class GarminActivityTest {

    private static final GarminActivity.ActivityType RUNNING = new GarminActivity.ActivityType("running", 1L, 17L);

    @Test
    void aMaximumBelowItsAverageIsDroppedButTheRunIsKept() {
        RunResult run = activity(150.0, 140.0, 170.0, 160.0, 100.0, 10.0).toRunResult().orElseThrow();

        assertThat(run.averageHeartRate()).isEqualTo(150);
        assertThat(run.metrics()).isEqualTo(new RunMetrics(null, 170, null, 100, 10));
    }

    @Test
    void zeroIsTreatedAsNotMeasured() {
        RunResult run = activity(null, 0.0, 0.0, 0.0, 0.0, 0.0).toRunResult().orElseThrow();

        // No cadence or stride without a sensor, but 0 m of climb is a real measurement.
        assertThat(run.metrics()).isEqualTo(new RunMetrics(null, null, null, null, 0));
    }

    private static GarminActivity activity(
            Double averageHR, Double maxHR, Double averageCadence, Double maxCadence, Double stride, Double climb) {
        return new GarminActivity(
                1L, "2026-10-01 06:00:00", RUNNING, 5000.0, 1500.0, averageHR,
                maxHR, averageCadence, maxCadence, stride, climb);
    }
}
