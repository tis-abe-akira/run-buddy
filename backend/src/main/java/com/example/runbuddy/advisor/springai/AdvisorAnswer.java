package com.example.runbuddy.advisor.springai;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * The JSON shape the model is asked to answer in. Kept apart from the domain: the model may send
 * anything, and only {@link #toPlan} decides whether it is a valid plan.
 */
record AdvisorAnswer(String reply, ProposedPlan proposedPlan) {

    record ProposedPlan(double targetKm, List<ProposedWorkout> workouts) {
    }

    record ProposedWorkout(LocalDate date, String type, double distanceKm, String note) {
    }

    /**
     * @throws IllegalArgumentException with a message the model can act on when the plan is invalid
     */
    MonthlyPlan toPlan(YearMonth month) {
        List<ProposedWorkout> items = proposedPlan.workouts() == null ? List.of() : proposedPlan.workouts();
        return new MonthlyPlan(
                month,
                Distance.ofKilometers(proposedPlan.targetKm()),
                items.stream().map(AdvisorAnswer::toWorkout).toList());
    }

    private static PlannedWorkout toWorkout(ProposedWorkout workout) {
        if (workout.date() == null || workout.type() == null) {
            throw new IllegalArgumentException("each workout needs a date and a type");
        }
        WorkoutType type;
        try {
            type = WorkoutType.valueOf(workout.type());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown workout type " + workout.type() + "; use one of " + List.of(WorkoutType.values()));
        }
        return new PlannedWorkout(workout.date(), type, Distance.ofKilometers(workout.distanceKm()), workout.note());
    }
}
