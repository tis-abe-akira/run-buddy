package com.example.runbuddy.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanAchievementTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Test
    void achievementRateIsActualOverTarget() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(100), List.of());
        List<RunResult> runs = List.of(run(2, 10, 60), run(4, 15, 90));

        assertThat(PlanAchievement.of(plan, runs).achievementRate()).hasValue(0.25);
    }

    @Test
    void achievementRateCanExceedOneHundredPercent() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(10), List.of());

        assertThat(PlanAchievement.of(plan, List.of(run(2, 15, 90))).achievementRate()).hasValue(1.5);
    }

    @Test
    void noTargetMeansNoAchievementRate() {
        PlanAchievement achievement = PlanAchievement.of(MonthlyPlan.empty(NOVEMBER), List.of(run(2, 10, 60)));

        assertThat(achievement.achievementRate()).isEmpty();
    }

    @Test
    void averagePaceWeighsRunsByDistance() {
        // 10 km in 50 min + 2 km in 14 min = 12 km in 64 min -> 5:20 /km
        List<RunResult> runs = List.of(run(2, 10, 50), run(3, 2, 14));

        PlanAchievement achievement = PlanAchievement.of(MonthlyPlan.empty(NOVEMBER), runs);

        assertThat(achievement.averagePace()).hasValue(Duration.ofSeconds(320));
    }

    @Test
    void noRunsMeansNoAveragePace() {
        assertThat(PlanAchievement.of(MonthlyPlan.empty(NOVEMBER), List.of()).averagePace()).isEmpty();
    }

    @Test
    void workoutCountsAsCompletedWhenARunHappenedThatDay() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ZERO, List.of(
                workout(2, 10), workout(4, 8), workout(6, 20)));
        List<RunResult> runs = List.of(run(2, 5, 30), run(6, 21, 120), run(7, 5, 30));

        PlanAchievement achievement = PlanAchievement.of(plan, runs);

        assertThat(achievement.plannedWorkoutCount()).isEqualTo(3);
        assertThat(achievement.completedWorkoutCount()).isEqualTo(2);
    }

    @Test
    void dailyProgressCoversEveryDayWithRunningTotals() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ZERO, List.of(workout(2, 10), workout(3, 5)));
        List<RunResult> runs = List.of(run(2, 8, 48), run(2, 4, 24), run(5, 6, 36));

        List<DailyProgress> daily = PlanAchievement.of(plan, runs).daily();

        assertThat(daily).hasSize(30);
        DailyProgress second = daily.get(1);
        assertThat(second.planned()).isEqualTo(Distance.ofKilometers(10));
        assertThat(second.actual()).isEqualTo(Distance.ofKilometers(12));
        DailyProgress last = daily.getLast();
        assertThat(last.cumulativePlanned()).isEqualTo(Distance.ofKilometers(15));
        assertThat(last.cumulativeActual()).isEqualTo(Distance.ofKilometers(18));
    }

    @Test
    void runOutsideThePlanMonthIsRejected() {
        RunResult december = new RunResult("x", LocalDate.of(2026, 12, 1).atTime(7, 0),
                Distance.ofKilometers(5), Duration.ofMinutes(30), null);

        assertThatThrownBy(() -> PlanAchievement.of(MonthlyPlan.empty(NOVEMBER), List.of(december)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static RunResult run(int day, double km, int minutes) {
        return new RunResult("run-" + day + "-" + km, LocalDate.of(2026, 11, day).atTime(7, 0),
                Distance.ofKilometers(km), Duration.ofMinutes(minutes), null);
    }

    private static PlannedWorkout workout(int day, double km) {
        return new PlannedWorkout(LocalDate.of(2026, 11, day), WorkoutType.EASY, Distance.ofKilometers(km), null);
    }
}
