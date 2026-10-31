package com.example.runbuddy.activity;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * One recorded run, as reported by an {@link ActivitySource}.
 *
 * @param activityId       identifier assigned by the source; used to make imports idempotent
 * @param averageHeartRate beats per minute, or {@code null} when the device did not record it
 * @param metrics          further measurements, each of which may be missing
 */
public record RunResult(
        String activityId,
        LocalDateTime startedAt,
        Distance distance,
        Duration duration,
        Integer averageHeartRate,
        RunMetrics metrics) {

    public RunResult {
        Objects.requireNonNull(activityId, "activityId");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(distance, "distance");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(metrics, "metrics");
        if (distance.isZero()) {
            throw new IllegalArgumentException("a run must cover some distance");
        }
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("a run must take some time");
        }
        if (averageHeartRate != null && metrics.maxHeartRate() != null && metrics.maxHeartRate() < averageHeartRate) {
            throw new IllegalArgumentException(
                    "maxHeartRate " + metrics.maxHeartRate() + " is below averageHeartRate " + averageHeartRate);
        }
    }

    /** A run with no measurements beyond distance, time and average heart rate. */
    public RunResult(
            String activityId, LocalDateTime startedAt, Distance distance, Duration duration, Integer averageHeartRate) {
        this(activityId, startedAt, distance, duration, averageHeartRate, RunMetrics.NONE);
    }

    public LocalDate date() {
        return startedAt.toLocalDate();
    }

    /** Time per kilometer. */
    public Duration pace() {
        return Duration.ofMillis(Math.round(duration.toMillis() / distance.toKilometers()));
    }

    public Optional<Integer> heartRate() {
        return Optional.ofNullable(averageHeartRate);
    }
}
