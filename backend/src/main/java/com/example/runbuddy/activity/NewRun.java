package com.example.runbuddy.activity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A run in the activity source that is not stored yet (#40), or was just stored: enough for the screen
 * to say so and to open its day.
 */
public record NewRun(String activityId, LocalDate date) {

    public NewRun {
        Objects.requireNonNull(activityId, "activityId");
        Objects.requireNonNull(date, "date");
    }

    static NewRun of(RunResult run) {
        return new NewRun(run.activityId(), run.date());
    }
}
