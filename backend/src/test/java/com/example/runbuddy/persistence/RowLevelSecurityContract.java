package com.example.runbuddy.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Users never see or overwrite each other's data, through any adapter. */
public interface RowLevelSecurityContract {

    YearMonth NOVEMBER = YearMonth.of(2026, 11);

    TestCurrentUser currentUser();

    MonthlyPlanRepository plans();

    RunResultRepository runs();

    @Test
    default void anotherUserSeesNothing() {
        String alice = currentUser().switchToNewUser();
        plans().save(plan(100));
        runs().saveAll(List.of(run("a")));

        currentUser().switchToNewUser();

        assertThat(plans().findByMonth(NOVEMBER)).isEmpty();
        assertThat(runs().findByMonth(NOVEMBER)).isEmpty();

        currentUser().switchTo(alice);
        assertThat(plans().findByMonth(NOVEMBER)).contains(plan(100));
    }

    @Test
    default void sameMonthAndActivityIdDoNotCollideAcrossUsers() {
        String alice = currentUser().switchToNewUser();
        plans().save(plan(100));
        runs().saveAll(List.of(run("a")));

        currentUser().switchToNewUser();
        plans().save(plan(200));
        int added = runs().saveAll(List.of(run("a")));

        assertThat(added).isEqualTo(1);
        currentUser().switchTo(alice);
        assertThat(plans().findByMonth(NOVEMBER)).contains(plan(100));
    }

    private static MonthlyPlan plan(double targetKm) {
        return new MonthlyPlan(NOVEMBER, Distance.ofKilometers(targetKm), List.of(
                new PlannedWorkout(NOVEMBER.atDay(1), WorkoutType.EASY, Distance.ofKilometers(5), "")));
    }

    private static RunResult run(String id) {
        return new RunResult(id, LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(5), Duration.ofMinutes(30), null);
    }
}
