package com.example.runbuddy.advisor;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.dashboard.PlanAchievement;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * What the advisor may look up while answering. Each call goes through the repositories, so it
 * runs in its own transaction and row level security limits it to the current user.
 *
 * <p>Results are plain records with kilometers and "m:ss" paces, which models read more reliably
 * than meters and milliseconds.
 */
@Component
public class AdvisorTools {

    private final MonthlyPlanRepository plans;
    private final RunResultRepository runs;

    public AdvisorTools(MonthlyPlanRepository plans, RunResultRepository runs) {
        this.plans = plans;
        this.runs = runs;
    }

    @Tool(description = "Summary of one month: target and actual distance, achievement rate, run counts and average pace.")
    public MonthSummary monthSummary(@ToolParam(description = "Month as YYYY-MM") String month) {
        YearMonth yearMonth = parse(month);
        MonthlyPlan plan = plans.findByMonth(yearMonth).orElseGet(() -> MonthlyPlan.empty(yearMonth));
        PlanAchievement achievement = PlanAchievement.of(plan, runs.findByMonth(yearMonth));
        var rate = achievement.achievementRate();
        return new MonthSummary(
                yearMonth.toString(),
                achievement.targetDistance().toKilometers(),
                achievement.plannedDistance().toKilometers(),
                achievement.actualDistance().toKilometers(),
                // One decimal: rounding 99.8% to "100%" made models report a missed target as met.
                rate.isPresent() ? String.format(Locale.ROOT, "%.1f%%", rate.getAsDouble() * 100) : "no target",
                achievement.runCount(),
                achievement.plannedWorkoutCount(),
                achievement.completedWorkoutCount(),
                achievement.averagePace().map(AdvisorTools::pace).orElse("-"));
    }

    @Tool(description = "Every run recorded in one month, oldest first.")
    public List<RunView> runs(@ToolParam(description = "Month as YYYY-MM") String month) {
        return runs.findByMonth(parse(month)).stream().map(AdvisorTools::view).toList();
    }

    @Tool(description = "The training plan of one month: distance target and planned workouts. Empty when not planned.")
    public PlanView plan(@ToolParam(description = "Month as YYYY-MM") String month) {
        YearMonth yearMonth = parse(month);
        MonthlyPlan plan = plans.findByMonth(yearMonth).orElseGet(() -> MonthlyPlan.empty(yearMonth));
        return new PlanView(
                yearMonth.toString(),
                plan.targetDistance().toKilometers(),
                plan.workouts().stream()
                        .map(w -> new PlannedWorkoutView(
                                w.date(), weekday(w.date()), w.type().name(), w.distance().toKilometers(), w.note()))
                        .toList());
    }

    private static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("month must look like 2026-11, got: " + month);
        }
    }

    private static RunView view(RunResult run) {
        return new RunView(
                run.date(),
                weekday(run.date()),
                run.distance().toKilometers(),
                Math.round(run.duration().toSeconds() / 60.0),
                pace(run.pace()),
                run.averageHeartRate());
    }

    /** Models get weekdays wrong when they work them out themselves, so every date carries one. */
    static String weekday(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private static String pace(Duration perKilometer) {
        long seconds = perKilometer.toSeconds();
        return "%d:%02d/km".formatted(seconds / 60, seconds % 60);
    }

    public record MonthSummary(
            String month,
            double targetKm,
            double plannedKm,
            double actualKm,
            String achievementRate,
            int runCount,
            int plannedWorkoutCount,
            int completedWorkoutCount,
            String averagePace) {
    }

    public record RunView(LocalDate date, String weekday, double distanceKm, long minutes, String pace, Integer averageHeartRate) {
    }

    public record PlanView(String month, double targetKm, List<PlannedWorkoutView> workouts) {
    }

    public record PlannedWorkoutView(LocalDate date, String weekday, String type, double distanceKm, String note) {
    }
}
