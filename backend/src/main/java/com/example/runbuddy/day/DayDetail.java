package com.example.runbuddy.day;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * One day: what was planned and every run of that day, oldest first. A day can hold several runs
 * (a morning and an evening run), so the day, not the run, is what the detail page shows.
 */
public record DayDetail(LocalDate date, List<PlannedWorkout> workouts, List<RunResult> runs) {

    public DayDetail {
        Objects.requireNonNull(date, "date");
        workouts = List.copyOf(workouts);
        runs = runs.stream().sorted(Comparator.comparing(RunResult::startedAt)).toList();
        if (workouts.stream().anyMatch(workout -> !workout.date().equals(date))
                || runs.stream().anyMatch(run -> !run.date().equals(date))) {
            throw new IllegalArgumentException("every workout and run must be on " + date);
        }
    }

    /** The day out of its month's plan and runs. */
    public static DayDetail of(LocalDate date, MonthlyPlan plan, List<RunResult> runsOfTheMonth) {
        if (!plan.month().equals(YearMonth.from(date))) {
            throw new IllegalArgumentException("the plan for " + plan.month() + " does not cover " + date);
        }
        return new DayDetail(
                date,
                plan.workouts().stream().filter(workout -> workout.date().equals(date)).toList(),
                runsOfTheMonth.stream().filter(run -> run.date().equals(date)).toList());
    }
}
