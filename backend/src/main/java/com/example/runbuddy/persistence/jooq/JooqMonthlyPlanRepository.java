package com.example.runbuddy.persistence.jooq;

import static com.example.runbuddy.persistence.jooq.generated.Tables.MONTHLY_PLAN;
import static com.example.runbuddy.persistence.jooq.generated.Tables.PLANNED_WORKOUT;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores a plan as one monthly_plan row plus its planned_workout rows.
 *
 * <p>Queries never mention user_id: row level security limits them to the current user, and the
 * column default fills it on insert.
 */
@Transactional
public class JooqMonthlyPlanRepository implements MonthlyPlanRepository {

    private final DSLContext dsl;

    public JooqMonthlyPlanRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(MonthlyPlan plan) {
        String month = plan.month().toString();
        long targetMeters = plan.targetDistance().meters();

        dsl.insertInto(MONTHLY_PLAN)
                .set(MONTHLY_PLAN.MONTH, month)
                .set(MONTHLY_PLAN.TARGET_METERS, targetMeters)
                .onConflict(MONTHLY_PLAN.USER_ID, MONTHLY_PLAN.MONTH)
                .doUpdate()
                .set(MONTHLY_PLAN.TARGET_METERS, targetMeters)
                .execute();

        // The workouts are part of the plan, so they are replaced as a whole.
        dsl.deleteFrom(PLANNED_WORKOUT).where(PLANNED_WORKOUT.MONTH.eq(month)).execute();
        List<PlannedWorkout> workouts = plan.workouts();
        if (workouts.isEmpty()) {
            return;
        }
        var insert = dsl.insertInto(
                PLANNED_WORKOUT,
                PLANNED_WORKOUT.MONTH,
                PLANNED_WORKOUT.POSITION,
                PLANNED_WORKOUT.WORKOUT_DATE,
                PLANNED_WORKOUT.TYPE,
                PLANNED_WORKOUT.DISTANCE_METERS,
                PLANNED_WORKOUT.NOTE);
        for (int position = 0; position < workouts.size(); position++) {
            PlannedWorkout workout = workouts.get(position);
            insert = insert.values(
                    month,
                    position,
                    workout.date(),
                    workout.type().name(),
                    workout.distance().meters(),
                    workout.note());
        }
        insert.execute();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MonthlyPlan> findByMonth(YearMonth month) {
        // MULTISET loads the plan and its workouts in a single query.
        return dsl.select(
                        MONTHLY_PLAN.TARGET_METERS,
                        multiset(select(
                                                PLANNED_WORKOUT.WORKOUT_DATE,
                                                PLANNED_WORKOUT.TYPE,
                                                PLANNED_WORKOUT.DISTANCE_METERS,
                                                PLANNED_WORKOUT.NOTE)
                                        .from(PLANNED_WORKOUT)
                                        .where(PLANNED_WORKOUT.USER_ID.eq(MONTHLY_PLAN.USER_ID))
                                        .and(PLANNED_WORKOUT.MONTH.eq(MONTHLY_PLAN.MONTH))
                                        .orderBy(PLANNED_WORKOUT.POSITION))
                                .convertFrom(rows -> rows.map(row -> new PlannedWorkout(
                                        row.value1(),
                                        WorkoutType.valueOf(row.value2()),
                                        Distance.ofMeters(row.value3()),
                                        row.value4()))))
                .from(MONTHLY_PLAN)
                .where(MONTHLY_PLAN.MONTH.eq(month.toString()))
                .fetchOptional(row -> new MonthlyPlan(month, Distance.ofMeters(row.value1()), row.value2()));
    }
}
