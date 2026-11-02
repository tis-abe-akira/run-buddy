package com.example.runbuddy.race;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A race the runner trains for. Finishing is always the goal; a target time ("sub-5") is an
 * optional second goal.
 *
 * @param targetTime finishing time aimed for, or {@code null} when the goal is to finish
 */
public record Race(UUID id, String name, LocalDate date, Distance distance, Duration targetTime) {

    public static final int MAX_NAME_LENGTH = 100;

    public Race {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(distance, "distance");
        name = name.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("a race needs a name");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("a race name is at most " + MAX_NAME_LENGTH + " characters");
        }
        if (distance.isZero()) {
            throw new IllegalArgumentException("a race must cover some distance");
        }
        if (targetTime != null && (targetTime.isNegative() || targetTime.isZero())) {
            throw new IllegalArgumentException("a target time must be positive");
        }
    }

    /** A new race with a fresh id. */
    public static Race plan(String name, LocalDate date, Distance distance, Duration targetTime) {
        return new Race(UUID.randomUUID(), name, date, distance, targetTime);
    }

    /**
     * Average time per kilometer that meets the target time, rounded down to the second so that
     * running at it finishes within the target. Empty when the goal is just to finish.
     */
    public Optional<Duration> targetPace() {
        if (targetTime == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds((long) Math.floor(targetTime.toSeconds() / distance.toKilometers())));
    }

    /** Days from {@code today} to the race: 0 on race day, negative once it is over. */
    public long daysFrom(LocalDate today) {
        return ChronoUnit.DAYS.between(today, date);
    }

    /** Still ahead, or being run today. */
    public boolean isUpcoming(LocalDate today) {
        return !date.isBefore(today);
    }
}
