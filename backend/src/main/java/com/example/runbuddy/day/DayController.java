package com.example.runbuddy.day;

import com.example.runbuddy.activity.RunController.RunResponse;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.PlanController.WorkoutBody;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The day detail page: one day's plan and runs with every measurement we have. */
@RestController
public class DayController {

    private final MonthlyPlanRepository plans;
    private final RunResultRepository runs;

    public DayController(MonthlyPlanRepository plans, RunResultRepository runs) {
        this.plans = plans;
        this.runs = runs;
    }

    @GetMapping("/api/days/{date}")
    public DayResponse get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        YearMonth month = YearMonth.from(date);
        DayDetail day = DayDetail.of(
                date,
                plans.findByMonth(month).orElseGet(() -> MonthlyPlan.empty(month)),
                runs.findByMonth(month));
        return new DayResponse(
                day.date(),
                day.workouts().stream().map(WorkoutBody::from).toList(),
                day.runs().stream().map(RunResponse::from).toList());
    }

    public record DayResponse(LocalDate date, List<WorkoutBody> plannedWorkouts, List<RunResponse> runs) {
    }
}
