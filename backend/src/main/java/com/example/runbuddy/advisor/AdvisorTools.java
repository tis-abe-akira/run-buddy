package com.example.runbuddy.advisor;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.dashboard.PlanAchievement;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.race.Race;
import com.example.runbuddy.race.RaceRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AdvisorTools.class);

    private final MonthlyPlanRepository plans;
    private final RunResultRepository runs;
    private final RaceRepository races;
    private final Clock clock;

    public AdvisorTools(MonthlyPlanRepository plans, RunResultRepository runs, RaceRepository races, Clock clock) {
        this.plans = plans;
        this.runs = runs;
        this.races = races;
        this.clock = clock;
    }

    @Tool(description = "Summary of one month: target and actual distance, achievement rate, run counts and average pace.")
    public MonthSummary monthSummary(@ToolParam(description = "Month as YYYY-MM") String month) {
        log.info("Advisor tool: monthSummary({})", month);
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
        log.info("Advisor tool: runs({})", month);
        return runs.findByMonth(parse(month)).stream().map(AdvisorTools::view).toList();
    }

    @Tool(description = "The training plan of one month: distance target and planned workouts. Empty when not planned.")
    public PlanView plan(@ToolParam(description = "Month as YYYY-MM") String month) {
        log.info("Advisor tool: plan({})", month);
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

    @Tool(description = "The runner's races from today on, earliest first: date, distance, goal (finish, or finish "
            + "within a target time and the pace that needs), and the days and whole weeks left. Empty when none is registered.")
    public List<RaceView> upcomingRaces() {
        log.info("Advisor tool: upcomingRaces()");
        LocalDate today = LocalDate.now(clock);
        return races.findAll().stream()
                .filter(race -> race.isUpcoming(today))
                .map(race -> {
                    long daysLeft = race.daysFrom(today);
                    return new RaceView(
                            race.name(),
                            race.date(),
                            weekday(race.date()),
                            race.distance().toKilometers(),
                            goal(race),
                            daysLeft,
                            daysLeft / 7);
                })
                .toList();
    }

    private static String goal(Race race) {
        if (race.targetTime() == null) {
            return "finish";
        }
        long seconds = race.targetTime().toSeconds();
        String time = "%d:%02d:%02d".formatted(seconds / 3600, seconds % 3600 / 60, seconds % 60);
        return "finish within %s (pace %s or faster)".formatted(time, pace(race.targetPace().orElseThrow()));
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

    public record RaceView(
            String name, LocalDate date, String weekday, double distanceKm, String goal, long daysLeft, long weeksLeft) {
    }

    public record PlannedWorkoutView(LocalDate date, String weekday, String type, double distanceKm, String note) {
    }
}
