package com.example.runbuddy.plan;

import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.util.Objects;

/** One session in a {@link MonthlyPlan}. */
public record PlannedWorkout(LocalDate date, WorkoutType type, Distance distance, String note) {

    public PlannedWorkout {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(distance, "distance");
        note = note == null ? "" : note.strip();
    }
}
