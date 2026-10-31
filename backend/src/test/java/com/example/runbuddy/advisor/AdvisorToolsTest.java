package com.example.runbuddy.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
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

class AdvisorToolsTest {

    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    private final InMemoryMonthlyPlanRepository plans = new InMemoryMonthlyPlanRepository();
    private final InMemoryRunResultRepository runs = new InMemoryRunResultRepository();
    private final AdvisorTools tools = new AdvisorTools(plans, runs);

    @Test
    void achievementRateKeepsOneDecimalSoANearMissIsNotReportedAsMet() {
        plans.save(new MonthlyPlan(OCTOBER, Distance.ofKilometers(220), List.of()));
        runs.saveAll(List.of(run(LocalDateTime.of(2026, 10, 4, 6, 0), 219.53)));

        assertThat(tools.monthSummary("2026-10").achievementRate()).isEqualTo("99.8%");
    }

    @Test
    void withoutATargetThereIsNoRate() {
        assertThat(tools.monthSummary("2026-10").achievementRate()).isEqualTo("no target");
    }

    @Test
    void runsAndPlannedWorkoutsCarryTheirWeekday() {
        runs.saveAll(List.of(run(LocalDateTime.of(2026, 10, 4, 6, 0), 10)));
        plans.save(new MonthlyPlan(OCTOBER, Distance.ZERO, List.of(
                new PlannedWorkout(LocalDate.of(2026, 10, 5), WorkoutType.EASY, Distance.ofKilometers(5), ""))));

        assertThat(tools.runs("2026-10")).singleElement().extracting(AdvisorTools.RunView::weekday).isEqualTo("Sun");
        assertThat(tools.plan("2026-10").workouts()).singleElement()
                .extracting(AdvisorTools.PlannedWorkoutView::weekday).isEqualTo("Mon");
    }

    @Test
    void malformedMonthIsExplainedToTheModel() {
        assertThatThrownBy(() -> tools.runs("October"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2026-11");
    }

    private static RunResult run(LocalDateTime startedAt, double km) {
        return new RunResult("r" + km, startedAt, Distance.ofKilometers(km), Duration.ofSeconds(Math.round(km * 330)), 150);
    }
}
