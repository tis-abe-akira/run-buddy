package com.example.runbuddy.plan;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * What every {@link MonthlyPlanRepository} must do, whatever stores the data. Implemented by the
 * test of each adapter (in memory, jOOQ, Spring Data JDBC), so all of them pass the same tests.
 */
public interface MonthlyPlanRepositoryContract {

    YearMonth NOVEMBER = YearMonth.of(2026, 11);

    MonthlyPlanRepository repository();

    @Test
    default void unplannedMonthIsAbsent() {
        assertThat(repository().findByMonth(NOVEMBER)).isEmpty();
    }

    @Test
    default void savedPlanIsReadBackUnchanged() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150.5), List.of(
                workout(8, WorkoutType.LONG, 20.25, "LSD"),
                workout(3, WorkoutType.EASY, 6, ""),
                workout(5, WorkoutType.INTERVAL, 10, "1000m × 5")));

        repository().save(plan);

        assertThat(repository().findByMonth(NOVEMBER)).contains(plan);
    }

    @Test
    default void planWithoutWorkoutsCanBeSaved() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(100), List.of());

        repository().save(plan);

        assertThat(repository().findByMonth(NOVEMBER)).contains(plan);
    }

    @Test
    default void savingAgainReplacesTheWholePlan() {
        repository().save(new MonthlyPlan(NOVEMBER, Distance.ofKilometers(100), List.of(
                workout(1, WorkoutType.EASY, 5, ""), workout(2, WorkoutType.EASY, 5, ""))));
        MonthlyPlan revised = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(120), List.of(
                workout(10, WorkoutType.TEMPO, 12, "")));

        repository().save(revised);

        assertThat(repository().findByMonth(NOVEMBER)).contains(revised);
    }

    @Test
    default void plansOfDifferentMonthsAreKeptApart() {
        MonthlyPlan november = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(100), List.of(
                workout(1, WorkoutType.EASY, 5, "")));
        MonthlyPlan december = new MonthlyPlan(NOVEMBER.plusMonths(1), Distance.ofKilometers(200), List.of(
                new PlannedWorkout(LocalDate.of(2026, 12, 1), WorkoutType.RACE, Distance.ofKilometers(42.195), "")));

        repository().save(november);
        repository().save(december);
        repository().save(november);

        assertThat(repository().findByMonth(NOVEMBER)).contains(november);
        assertThat(repository().findByMonth(NOVEMBER.plusMonths(1))).contains(december);
    }

    private static PlannedWorkout workout(int day, WorkoutType type, double km, String note) {
        return new PlannedWorkout(NOVEMBER.atDay(day), type, Distance.ofKilometers(km), note);
    }
}
