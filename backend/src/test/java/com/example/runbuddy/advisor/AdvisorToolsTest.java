package com.example.runbuddy.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.race.InMemoryRaceRepository;
import com.example.runbuddy.race.Race;
import com.example.runbuddy.shared.Distance;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;

class AdvisorToolsTest {

    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    private final InMemoryMonthlyPlanRepository plans = new InMemoryMonthlyPlanRepository();
    private final InMemoryRunResultRepository runs = new InMemoryRunResultRepository();
    private final InMemoryRaceRepository races = new InMemoryRaceRepository();
    /** Today is 1 Nov 2026. */
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
    private final AdvisorTools tools = new AdvisorTools(plans, runs, races, clock);

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
    void upcomingRacesTellTheGoalTheTimeLeftAndThePaceNeeded() {
        races.save(Race.plan("Autumn marathon", LocalDate.of(2026, 11, 22), Distance.ofMeters(42_195), Duration.ofHours(5)));
        races.save(Race.plan("Spring half", LocalDate.of(2027, 3, 14), Distance.ofMeters(21_098), null));

        assertThat(tools.upcomingRaces()).satisfiesExactly(
                marathon -> {
                    assertThat(marathon.name()).isEqualTo("Autumn marathon");
                    assertThat(marathon.weekday()).isEqualTo("Sun");
                    assertThat(marathon.distanceKm()).isEqualTo(42.195);
                    assertThat(marathon.goal()).isEqualTo("finish within 5:00:00 (pace 7:06/km or faster)");
                    assertThat(marathon.daysLeft()).isEqualTo(21);
                    assertThat(marathon.weeksLeft()).isEqualTo(3);
                },
                half -> assertThat(half.goal()).isEqualTo("finish"));
    }

    @Test
    void theModelIsOfferedEveryTool() {
        assertThat(ToolCallbacks.from(tools))
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder("monthSummary", "runs", "plan", "upcomingRaces");
    }

    @Test
    void racesAlreadyRunAreNotUpcoming() {
        races.save(Race.plan("Last month's 10k", LocalDate.of(2026, 10, 4), Distance.ofKilometers(10), null));
        races.save(Race.plan("Today's 5k", LocalDate.of(2026, 11, 1), Distance.ofKilometers(5), null));

        assertThat(tools.upcomingRaces()).extracting(AdvisorTools.RaceView::name).containsExactly("Today's 5k");
    }

    @Test
    void withoutRacesThereIsNothingUpcoming() {
        assertThat(tools.upcomingRaces()).isEmpty();
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
