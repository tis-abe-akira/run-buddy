package com.example.runbuddy.export;

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

class DailyLogTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Test
    void hasALineForEveryDayEvenWithNothingPlannedOrRun() {
        DailyLog log = DailyLog.of(MonthlyPlan.empty(NOVEMBER), List.of());

        assertThat(log.days()).hasSize(30);
        assertThat(log.days().getFirst().date()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(log.days().getFirst().workouts()).isEmpty();
        assertThat(log.days().getFirst().runs()).isEmpty();
    }

    @Test
    void combinesEverythingOnTheSameDay() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(200), List.of(
                workout(3, WorkoutType.EASY, 5), workout(3, WorkoutType.TEMPO, 8)));
        List<RunResult> runs = List.of(run("a", 3, 6, 33, 140), run("b", 3, 4, 24, 160));

        DailyLog.Day third = DailyLog.of(plan, runs).days().get(2);

        assertThat(third.plannedDistance()).isEqualTo(Distance.ofKilometers(13));
        assertThat(third.actualDistance()).isEqualTo(Distance.ofKilometers(10));
        assertThat(third.totalTime()).isEqualTo(Duration.ofMinutes(57));
        // 57 min over 10 km
        assertThat(third.pace()).hasValue(Duration.ofSeconds(342));
    }

    @Test
    void heartRateIsWeightedByDurationAndSkipsRunsWithoutOne() {
        List<RunResult> runs = List.of(
                run("long", 3, 10, 60, 140), run("short", 3, 2, 20, 160), run("none", 3, 5, 30, null));

        DailyLog.Day third = DailyLog.of(MonthlyPlan.empty(NOVEMBER), runs).days().get(2);

        // (140 * 60 + 160 * 20) / 80 = 145
        assertThat(third.averageHeartRate()).hasValue(145);
    }

    @Test
    void noHeartRateWhenNoRunRecordedOne() {
        DailyLog.Day third = DailyLog.of(MonthlyPlan.empty(NOVEMBER), List.of(run("a", 3, 5, 30, null))).days().get(2);

        assertThat(third.averageHeartRate()).isEmpty();
    }

    @Test
    void monthTotals() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(200), List.of(
                workout(1, WorkoutType.LONG, 20), workout(3, WorkoutType.EASY, 8)));
        List<RunResult> runs = List.of(run("a", 1, 21, 120, 150), run("b", 4, 9, 50, 145));

        DailyLog log = DailyLog.of(plan, runs);

        assertThat(log.targetDistance()).isEqualTo(Distance.ofKilometers(200));
        assertThat(log.plannedDistance()).isEqualTo(Distance.ofKilometers(28));
        assertThat(log.actualDistance()).isEqualTo(Distance.ofKilometers(30));
        assertThat(log.runCount()).isEqualTo(2);
        assertThat(log.totalTime()).isEqualTo(Duration.ofMinutes(170));
        assertThat(log.averagePace()).hasValue(Duration.ofSeconds(340));
    }

    @Test
    void runOutsideTheMonthIsRejected() {
        RunResult december = new RunResult("x", LocalDate.of(2026, 12, 1).atTime(6, 0),
                Distance.ofKilometers(5), Duration.ofMinutes(30), null);

        assertThatThrownBy(() -> DailyLog.of(MonthlyPlan.empty(NOVEMBER), List.of(december)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static PlannedWorkout workout(int day, WorkoutType type, double km) {
        return new PlannedWorkout(NOVEMBER.atDay(day), type, Distance.ofKilometers(km), "");
    }

    static RunResult run(String id, int day, double km, int minutes, Integer heartRate) {
        return new RunResult(id, NOVEMBER.atDay(day).atTime(6, 30), Distance.ofKilometers(km), Duration.ofMinutes(minutes), heartRate);
    }
}
