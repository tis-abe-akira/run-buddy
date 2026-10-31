package com.example.runbuddy.plan;

import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans/{month}")
public class PlanController {

    private final MonthlyPlanRepository monthlyPlanRepository;

    public PlanController(MonthlyPlanRepository monthlyPlanRepository) {
        this.monthlyPlanRepository = monthlyPlanRepository;
    }

    @GetMapping
    public PlanBody get(@PathVariable YearMonth month) {
        return PlanBody.from(monthlyPlanRepository.findByMonth(month).orElseGet(() -> MonthlyPlan.empty(month)));
    }

    @PutMapping
    public PlanBody put(@PathVariable YearMonth month, @RequestBody PlanBody body) {
        MonthlyPlan plan = body.toPlan(month);
        monthlyPlanRepository.save(plan);
        return PlanBody.from(plan);
    }

    /** Request and response shape for a plan. Distances are in kilometers. */
    public record PlanBody(double targetKm, List<WorkoutBody> workouts) {

        static PlanBody from(MonthlyPlan plan) {
            return new PlanBody(
                    plan.targetDistance().toKilometers(),
                    plan.workouts().stream().map(WorkoutBody::from).toList());
        }

        MonthlyPlan toPlan(YearMonth month) {
            List<WorkoutBody> items = workouts == null ? List.of() : workouts;
            return new MonthlyPlan(
                    month,
                    Distance.ofKilometers(targetKm),
                    items.stream().map(WorkoutBody::toWorkout).toList());
        }
    }

    public record WorkoutBody(LocalDate date, WorkoutType type, double distanceKm, String note) {

        public static WorkoutBody from(PlannedWorkout workout) {
            return new WorkoutBody(
                    workout.date(), workout.type(), workout.distance().toKilometers(), workout.note());
        }

        PlannedWorkout toWorkout() {
            if (date == null || type == null) {
                throw new IllegalArgumentException("each workout needs a date and a type");
            }
            return new PlannedWorkout(date, type, Distance.ofKilometers(distanceKm), note);
        }
    }
}
