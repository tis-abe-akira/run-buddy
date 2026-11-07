package com.example.runbuddy.dashboard;

import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.race.RaceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

/**
 * Reads what {@link SituationRules} looks at. Each read is its own short transaction through the
 * repositories, so nothing here holds a transaction open (the AI step in #28 runs outside one).
 */
@Component
public class TrainingSnapshots {

    private final MonthlyPlanRepository plans;
    private final RunResultRepository runs;
    private final RaceRepository races;
    private final Clock clock;

    public TrainingSnapshots(MonthlyPlanRepository plans, RunResultRepository runs, RaceRepository races, Clock clock) {
        this.plans = plans;
        this.runs = runs;
        this.races = races;
        this.clock = clock;
    }

    public TrainingSnapshot today() {
        LocalDate today = LocalDate.now(clock);
        YearMonth month = YearMonth.from(today);
        PlanAchievement thisMonth = PlanAchievement.of(
                plans.findByMonth(month).orElseGet(() -> MonthlyPlan.empty(month)), runs.findByMonth(month));
        boolean nextMonthPlanned = plans.findByMonth(month.plusMonths(1))
                .filter(plan -> !plan.targetDistance().isZero() || !plan.workouts().isEmpty())
                .isPresent();
        return new TrainingSnapshot(
                today, thisMonth, nextMonthPlanned, races.findAll(), runs.latestStart().map(LocalDateTime::toLocalDate));
    }
}
