package com.example.runbuddy.plan;

import com.example.runbuddy.shared.Distance;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The training plan for one month: a distance goal and the sessions intended to reach it.
 *
 * <p>The goal is set separately from the sessions, because runners often aim for a monthly total
 * without scheduling every run.
 */
public record MonthlyPlan(YearMonth month, Distance targetDistance, List<PlannedWorkout> workouts) {

    public MonthlyPlan {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(targetDistance, "targetDistance");
        Objects.requireNonNull(workouts, "workouts");
        for (PlannedWorkout workout : workouts) {
            if (!YearMonth.from(workout.date()).equals(month)) {
                throw new IllegalArgumentException(
                        "workout on " + workout.date() + " is outside the plan month " + month);
            }
        }
        workouts = workouts.stream().sorted(Comparator.comparing(PlannedWorkout::date)).toList();
    }

    /** A month nobody has planned yet. */
    public static MonthlyPlan empty(YearMonth month) {
        return new MonthlyPlan(month, Distance.ZERO, List.of());
    }

    public Distance plannedDistance() {
        return Distance.sum(workouts.stream().map(PlannedWorkout::distance).toList());
    }
}
