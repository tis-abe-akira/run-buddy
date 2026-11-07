package com.example.runbuddy.plan;

import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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

    /**
     * This plan revised on {@code today} with {@code proposal} (#31): the days before today stay as they
     * were planned, so the plan can still be compared with what was run; today and the days after come
     * from the proposal, and whatever it put on days that have passed is dropped. The target comes from
     * the proposal, as the goal for the whole month. A month that is over is not revised.
     *
     * @throws IllegalArgumentException if the proposal is for another month
     */
    public MonthlyPlan revisedFrom(LocalDate today, MonthlyPlan proposal) {
        if (!proposal.month().equals(month)) {
            throw new IllegalArgumentException("a revision of " + month + " cannot use a plan for " + proposal.month());
        }
        if (today.isAfter(month.atEndOfMonth())) {
            return this;
        }
        List<PlannedWorkout> kept = workouts.stream().filter(w -> w.date().isBefore(today)).toList();
        List<PlannedWorkout> ahead = proposal.workouts().stream().filter(w -> !w.date().isBefore(today)).toList();
        return new MonthlyPlan(month, proposal.targetDistance(), Stream.concat(kept.stream(), ahead.stream()).toList());
    }

    public Distance plannedDistance() {
        return Distance.sum(workouts.stream().map(PlannedWorkout::distance).toList());
    }
}
