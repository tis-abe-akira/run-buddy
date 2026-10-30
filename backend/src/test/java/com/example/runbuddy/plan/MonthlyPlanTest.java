package com.example.runbuddy.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class MonthlyPlanTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Test
    void workoutsAreKeptInDateOrder() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of(
                workout(LocalDate.of(2026, 11, 20), 10),
                workout(LocalDate.of(2026, 11, 2), 5)));

        assertThat(plan.workouts()).extracting(PlannedWorkout::date)
                .containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20));
    }

    @Test
    void plannedDistanceIsTheSumOfWorkouts() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of(
                workout(LocalDate.of(2026, 11, 2), 5),
                workout(LocalDate.of(2026, 11, 8), 18.5)));

        assertThat(plan.plannedDistance()).isEqualTo(Distance.ofKilometers(23.5));
    }

    @Test
    void workoutOutsideTheMonthIsRejected() {
        List<PlannedWorkout> workouts = List.of(workout(LocalDate.of(2026, 12, 1), 5));

        assertThatThrownBy(() -> new MonthlyPlan(NOVEMBER, Distance.ZERO, workouts))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2026-12-01");
    }

    @Test
    void emptyPlanHasNoTargetAndNoWorkouts() {
        MonthlyPlan plan = MonthlyPlan.empty(NOVEMBER);

        assertThat(plan.targetDistance().isZero()).isTrue();
        assertThat(plan.workouts()).isEmpty();
    }

    private static PlannedWorkout workout(LocalDate date, double km) {
        return new PlannedWorkout(date, WorkoutType.EASY, Distance.ofKilometers(km), null);
    }
}
