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
 */
public record RunResult(
        String activityId,
        LocalDateTime startedAt,
        Distance distance,
        Duration duration,
        Integer averageHeartRate) {

    public RunResult {
        Objects.requireNonNull(activityId, "activityId");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(distance, "distance");
        Objects.requireNonNull(duration, "duration");
        if (distance.isZero()) {
            throw new IllegalArgumentException("a run must cover some distance");
        }
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("a run must take some time");
        }
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
