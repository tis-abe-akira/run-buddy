package com.example.runbuddy.activity;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * One lap of a run, as the watch recorded it (ADR-0012). Optional measurements are null when the
 * watch did not record them.
 *
 * @param number              1 for the first lap
 * @param startOffset         time from the start of the run to the start of the lap
 * @param duration            timer time (pauses excluded)
 * @param averageCadence      steps per minute, both feet
 * @param maxCadence          steps per minute, both feet
 * @param elevationGainMeters climb during the lap
 */
public record Lap(
        int number,
        Duration startOffset,
        Distance distance,
        Duration duration,
        Integer averageHeartRate,
        Integer maxHeartRate,
        Integer averageCadence,
        Integer maxCadence,
        Integer elevationGainMeters) {

    public Lap {
        Objects.requireNonNull(startOffset, "startOffset");
        Objects.requireNonNull(distance, "distance");
        Objects.requireNonNull(duration, "duration");
        if (number < 1) {
            throw new IllegalArgumentException("laps are numbered from 1: " + number);
        }
        if (startOffset.isNegative() || duration.isNegative()) {
            throw new IllegalArgumentException("a lap cannot start before the run or last a negative time");
        }
        requirePositive(averageHeartRate, "averageHeartRate");
        requirePositive(maxHeartRate, "maxHeartRate");
        requirePositive(averageCadence, "averageCadence");
        requirePositive(maxCadence, "maxCadence");
        if (elevationGainMeters != null && elevationGainMeters < 0) {
            throw new IllegalArgumentException("elevationGainMeters must not be negative: " + elevationGainMeters);
        }
        requireMaxNotBelowAverage(maxHeartRate, averageHeartRate, "heart rate");
        requireMaxNotBelowAverage(maxCadence, averageCadence, "cadence");
    }

    /** Time per kilometer; empty for a lap without distance (a pause or a lap pressed twice). */
    public Optional<Duration> pace() {
        if (distance.isZero() || duration.isZero()) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofMillis(Math.round(duration.toMillis() / distance.toKilometers())));
    }

    private static void requirePositive(Integer value, String name) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(name + " must be positive: " + value);
        }
    }

    private static void requireMaxNotBelowAverage(Integer max, Integer average, String what) {
        if (max != null && average != null && max < average) {
            throw new IllegalArgumentException("max " + what + " " + max + " is below the average " + average);
        }
    }
}
