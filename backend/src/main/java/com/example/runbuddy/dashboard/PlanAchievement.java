package com.example.runbuddy.dashboard;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

/** How a month's runs compare with its plan. */
public final class PlanAchievement {

    private final MonthlyPlan plan;
    private final List<RunResult> runs;

    private PlanAchievement(MonthlyPlan plan, List<RunResult> runs) {
        this.plan = plan;
        this.runs = List.copyOf(runs);
    }

    /**
     * @throws IllegalArgumentException if a run falls outside the plan month
     */
    public static PlanAchievement of(MonthlyPlan plan, List<RunResult> runs) {
        for (RunResult run : runs) {
            if (!YearMonth.from(run.date()).equals(plan.month())) {
                throw new IllegalArgumentException(
                        "run on " + run.date() + " is outside the plan month " + plan.month());
            }
        }
        return new PlanAchievement(plan, runs);
    }

    public YearMonth month() {
        return plan.month();
    }

    public Distance targetDistance() {
        return plan.targetDistance();
    }

    public Distance plannedDistance() {
        return plan.plannedDistance();
    }

    public Distance actualDistance() {
        return Distance.sum(runs.stream().map(RunResult::distance).toList());
    }

    /** Actual distance divided by the target; empty when no target is set. Can exceed 1.0. */
    public OptionalDouble achievementRate() {
        if (targetDistance().isZero()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((double) actualDistance().meters() / targetDistance().meters());
    }

    public int runCount() {
        return runs.size();
    }

    public int plannedWorkoutCount() {
        return plan.workouts().size();
    }

    /** Planned workouts on a day with at least one run. Distance and type are not compared yet. */
    public int completedWorkoutCount() {
        Set<LocalDate> runDays = runs.stream().map(RunResult::date).collect(Collectors.toSet());
        return (int) plan.workouts().stream().map(PlannedWorkout::date).filter(runDays::contains).count();
    }

    /** Overall pace across all runs (total time over total distance), empty when nothing was run. */
    public Optional<Duration> averagePace() {
        Distance total = actualDistance();
        if (total.isZero()) {
            return Optional.empty();
        }
        long totalMillis = runs.stream().mapToLong(run -> run.duration().toMillis()).sum();
        return Optional.of(Duration.ofMillis(Math.round(totalMillis / total.toKilometers())));
    }

    /** One entry per calendar day of the month. */
    public List<DailyProgress> daily() {
        Map<LocalDate, Distance> plannedByDay = plan.workouts().stream()
                .collect(Collectors.groupingBy(
                        PlannedWorkout::date,
                        Collectors.reducing(Distance.ZERO, PlannedWorkout::distance, Distance::plus)));
        Map<LocalDate, Distance> actualByDay = runs.stream()
                .collect(Collectors.groupingBy(
                        RunResult::date,
                        Collectors.reducing(Distance.ZERO, RunResult::distance, Distance::plus)));

        List<DailyProgress> days = new ArrayList<>();
        Distance cumulativePlanned = Distance.ZERO;
        Distance cumulativeActual = Distance.ZERO;
        YearMonth month = plan.month();
        for (LocalDate day = month.atDay(1); !day.isAfter(month.atEndOfMonth()); day = day.plusDays(1)) {
            Distance planned = plannedByDay.getOrDefault(day, Distance.ZERO);
            Distance actual = actualByDay.getOrDefault(day, Distance.ZERO);
            cumulativePlanned = cumulativePlanned.plus(planned);
            cumulativeActual = cumulativeActual.plus(actual);
            days.add(new DailyProgress(day, planned, actual, cumulativePlanned, cumulativeActual));
        }
        return days;
    }
}
