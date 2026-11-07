package com.example.runbuddy.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class RevisedFrom {

        private final MonthlyPlan current = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of(
                workout(day(3), 6), workout(day(15), 8), workout(day(20), 15)));

        @Test
        void keepsTheDaysBeforeTodayAndTakesTheRestFromTheProposal() {
            MonthlyPlan proposal = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(120), List.of(
                    workout(day(16), 10), workout(day(25), 20)));

            MonthlyPlan revised = current.revisedFrom(day(15), proposal);

            assertThat(revised.workouts()).extracting(PlannedWorkout::date).containsExactly(day(3), day(16), day(25));
            assertThat(revised.targetDistance()).isEqualTo(Distance.ofKilometers(120));
        }

        @Test
        void dropsWhatTheProposalPutsOnDaysThatHavePassed() {
            MonthlyPlan proposal = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of(
                    workout(day(1), 30), workout(day(14), 30), workout(day(18), 10)));

            MonthlyPlan revised = current.revisedFrom(day(15), proposal);

            assertThat(revised.workouts()).extracting(PlannedWorkout::date).containsExactly(day(3), day(18));
        }

        @Test
        void todayIsStillOpenToRevision() {
            MonthlyPlan proposal = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of(workout(day(15), 12)));

            MonthlyPlan revised = current.revisedFrom(day(15), proposal);

            assertThat(revised.workouts()).containsExactly(workout(day(3), 6), workout(day(15), 12));
        }

        @Test
        void aMonthThatHasNotStartedIsTakenWholeFromTheProposal() {
            MonthlyPlan proposal = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(100), List.of(workout(day(1), 5)));

            assertThat(current.revisedFrom(LocalDate.of(2026, 10, 31), proposal)).isEqualTo(proposal);
        }

        @Test
        void aMonthThatIsOverStaysAsItWas() {
            MonthlyPlan proposal = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(100), List.of(workout(day(30), 5)));

            assertThat(current.revisedFrom(LocalDate.of(2026, 12, 1), proposal)).isEqualTo(current);
        }

        @Test
        void onlyRevisesWithAProposalForTheSameMonth() {
            MonthlyPlan december = MonthlyPlan.empty(NOVEMBER.plusMonths(1));

            assertThatThrownBy(() -> current.revisedFrom(day(15), december))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2026-12");
        }

        private LocalDate day(int dayOfMonth) {
            return NOVEMBER.atDay(dayOfMonth);
        }
    }

    private static PlannedWorkout workout(LocalDate date, double km) {
        return new PlannedWorkout(date, WorkoutType.EASY, Distance.ofKilometers(km), null);
    }
}
