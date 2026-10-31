package com.example.runbuddy.persistence.jdbc;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistence shape of a {@link MonthlyPlan} aggregate. user_id is not mapped: the database fills
 * it and row level security scopes every statement, so "month" alone identifies the row here.
 *
 * <p>Kept apart from the domain record so that the domain carries no mapping annotations.
 */
@Table("monthly_plan")
record MonthlyPlanRow(
        @Id String month,
        long targetMeters,
        @MappedCollection(idColumn = "month", keyColumn = "position") List<WorkoutRow> workouts) {

    static MonthlyPlanRow from(MonthlyPlan plan) {
        return new MonthlyPlanRow(
                plan.month().toString(),
                plan.targetDistance().meters(),
                plan.workouts().stream().map(WorkoutRow::from).toList());
    }

    MonthlyPlan toPlan() {
        return new MonthlyPlan(
                YearMonth.parse(month), Distance.ofMeters(targetMeters), workouts.stream().map(WorkoutRow::toWorkout).toList());
    }

    @Table("planned_workout")
    record WorkoutRow(LocalDate workoutDate, String type, long distanceMeters, String note) {

        static WorkoutRow from(PlannedWorkout workout) {
            return new WorkoutRow(workout.date(), workout.type().name(), workout.distance().meters(), workout.note());
        }

        PlannedWorkout toWorkout() {
            return new PlannedWorkout(workoutDate, WorkoutType.valueOf(type), Distance.ofMeters(distanceMeters), note);
        }
    }
}
