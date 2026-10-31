package com.example.runbuddy.dashboard;

import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final MonthlyPlanRepository monthlyPlanRepository;
    private final RunResultRepository runResultRepository;

    public DashboardController(
            MonthlyPlanRepository monthlyPlanRepository, RunResultRepository runResultRepository) {
        this.monthlyPlanRepository = monthlyPlanRepository;
        this.runResultRepository = runResultRepository;
    }

    @GetMapping("/api/dashboard/{month}")
    public DashboardResponse get(@PathVariable YearMonth month) {
        PlanAchievement achievement = PlanAchievement.of(
                monthlyPlanRepository.findByMonth(month).orElseGet(() -> MonthlyPlan.empty(month)), runResultRepository.findByMonth(month));
        return DashboardResponse.from(achievement);
    }

    /** Distances are in kilometers. Nullable fields are null when there is nothing to compute. */
    public record DashboardResponse(
            YearMonth month,
            double targetKm,
            double plannedKm,
            double actualKm,
            @Schema(types = {"number", "null"}) Double achievementRate,
            int runCount,
            int plannedWorkoutCount,
            int completedWorkoutCount,
            @Schema(types = {"integer", "null"}) Long averagePaceSecondsPerKm,
            List<DayBody> daily) {

        static DashboardResponse from(PlanAchievement achievement) {
            var rate = achievement.achievementRate();
            return new DashboardResponse(
                    achievement.month(),
                    achievement.targetDistance().toKilometers(),
                    achievement.plannedDistance().toKilometers(),
                    achievement.actualDistance().toKilometers(),
                    rate.isPresent() ? rate.getAsDouble() : null,
                    achievement.runCount(),
                    achievement.plannedWorkoutCount(),
                    achievement.completedWorkoutCount(),
                    achievement.averagePace().map(Duration::toSeconds).orElse(null),
                    achievement.daily().stream().map(DayBody::from).toList());
        }
    }

    public record DayBody(
            LocalDate date,
            double plannedKm,
            double actualKm,
            double cumulativePlannedKm,
            double cumulativeActualKm) {

        static DayBody from(DailyProgress day) {
            return new DayBody(
                    day.date(),
                    day.planned().toKilometers(),
                    day.actual().toKilometers(),
                    day.cumulativePlanned().toKilometers(),
                    day.cumulativeActual().toKilometers());
        }
    }
}
