package com.example.runbuddy.export;

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
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * A month as one line per calendar day, plan next to what was run, for spreadsheets.
 *
 * <p>Several workouts or runs on one day are combined into that day's line.
 */
public record DailyLog(YearMonth month, Distance targetDistance, List<Day> days) {

    public DailyLog {
        days = List.copyOf(days);
    }

    /**
     * @throws IllegalArgumentException if a run falls outside the plan month
     */
    public static DailyLog of(MonthlyPlan plan, List<RunResult> runs) {
        YearMonth month = plan.month();
        for (RunResult run : runs) {
            if (!YearMonth.from(run.date()).equals(month)) {
                throw new IllegalArgumentException("run on " + run.date() + " is outside " + month);
            }
        }
        Map<LocalDate, List<PlannedWorkout>> workoutsByDay =
                plan.workouts().stream().collect(Collectors.groupingBy(PlannedWorkout::date));
        Map<LocalDate, List<RunResult>> runsByDay = runs.stream().collect(Collectors.groupingBy(RunResult::date));

        List<Day> days = new ArrayList<>();
        for (LocalDate date = month.atDay(1); !date.isAfter(month.atEndOfMonth()); date = date.plusDays(1)) {
            days.add(new Day(date, workoutsByDay.getOrDefault(date, List.of()), runsByDay.getOrDefault(date, List.of())));
        }
        return new DailyLog(month, plan.targetDistance(), days);
    }

    public Distance plannedDistance() {
        return Distance.sum(days.stream().map(Day::plannedDistance).toList());
    }

    public Distance actualDistance() {
        return Distance.sum(days.stream().map(Day::actualDistance).toList());
    }

    public int runCount() {
        return days.stream().mapToInt(day -> day.runs().size()).sum();
    }

    public Duration totalTime() {
        return days.stream().map(Day::totalTime).reduce(Duration.ZERO, Duration::plus);
    }

    /** Total time over total distance; empty when nothing was run. */
    public Optional<Duration> averagePace() {
        return pace(totalTime(), actualDistance());
    }

    /** One calendar day: what was planned and what was run. */
    public record Day(LocalDate date, List<PlannedWorkout> workouts, List<RunResult> runs) {

        public Day {
            workouts = List.copyOf(workouts);
            runs = List.copyOf(runs);
        }

        public Distance plannedDistance() {
            return Distance.sum(workouts.stream().map(PlannedWorkout::distance).toList());
        }

        public Distance actualDistance() {
            return Distance.sum(runs.stream().map(RunResult::distance).toList());
        }

        public Duration totalTime() {
            return runs.stream().map(RunResult::duration).reduce(Duration.ZERO, Duration::plus);
        }

        public Optional<Duration> pace() {
            return DailyLog.pace(totalTime(), actualDistance());
        }

        /**
         * Average heart rate weighted by each run's duration, over the runs that recorded one, so
         * a long run counts for more than a short one.
         */
        public OptionalLong averageHeartRate() {
            long weightedSum = 0;
            long seconds = 0;
            for (RunResult run : runs) {
                if (run.averageHeartRate() != null) {
                    weightedSum += (long) run.averageHeartRate() * run.duration().toSeconds();
                    seconds += run.duration().toSeconds();
                }
            }
            return seconds == 0 ? OptionalLong.empty() : OptionalLong.of(Math.round((double) weightedSum / seconds));
        }
    }

    private static Optional<Duration> pace(Duration time, Distance distance) {
        if (distance.isZero()) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(Math.round(time.toSeconds() / distance.toKilometers())));
    }
}
