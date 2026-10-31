package com.example.runbuddy.day;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class DayDetailTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final LocalDate DAY = LocalDate.of(2026, 11, 8);

    @Test
    void takesTheDaysWorkoutsAndRunsFromTheMonth() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of(
                workout(7, WorkoutType.EASY), workout(8, WorkoutType.LONG), workout(9, WorkoutType.EASY)));
        List<RunResult> runs = List.of(run("other", 7, 6), run("morning", 8, 6), run("evening", 8, 18));

        DayDetail day = DayDetail.of(DAY, plan, runs);

        assertThat(day.date()).isEqualTo(DAY);
        assertThat(day.workouts()).extracting(PlannedWorkout::type).containsExactly(WorkoutType.LONG);
        assertThat(day.runs()).extracting(RunResult::activityId).containsExactly("morning", "evening");
    }

    @Test
    void runsOfTheDayAreInTheOrderTheyStarted() {
        DayDetail day = DayDetail.of(DAY, MonthlyPlan.empty(NOVEMBER), List.of(run("evening", 8, 18), run("morning", 8, 6)));

        assertThat(day.runs()).extracting(RunResult::activityId).containsExactly("morning", "evening");
    }

    @Test
    void aDayWithNothingPlannedOrRunIsEmpty() {
        DayDetail day = DayDetail.of(DAY, MonthlyPlan.empty(NOVEMBER), List.of());

        assertThat(day.workouts()).isEmpty();
        assertThat(day.runs()).isEmpty();
    }

    @Test
    void thePlanMustBeForTheDaysMonth() {
        assertThatThrownBy(() -> DayDetail.of(DAY, MonthlyPlan.empty(YearMonth.of(2026, 12)), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static PlannedWorkout workout(int day, WorkoutType type) {
        return new PlannedWorkout(NOVEMBER.atDay(day), type, Distance.ofKilometers(10), "");
    }

    private static RunResult run(String id, int day, int hour) {
        return new RunResult(id, LocalDateTime.of(2026, 11, day, hour, 0),
                Distance.ofKilometers(8), Duration.ofMinutes(45), 150);
    }
}
