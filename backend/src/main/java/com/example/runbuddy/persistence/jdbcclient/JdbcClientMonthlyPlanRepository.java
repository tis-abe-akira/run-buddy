package com.example.runbuddy.persistence.jdbcclient;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/** See JooqMonthlyPlanRepository: same SQL, written as strings. user_id is left to the database. */
@Transactional
public class JdbcClientMonthlyPlanRepository implements MonthlyPlanRepository {

    private final JdbcClient jdbc;
    // JdbcClient has no batch API, so batches go through the template underneath it.
    private final NamedParameterJdbcTemplate batch;

    public JdbcClientMonthlyPlanRepository(JdbcClient jdbc, NamedParameterJdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    @Override
    public void save(MonthlyPlan plan) {
        String month = plan.month().toString();
        jdbc.sql("""
                        insert into monthly_plan (month, target_meters) values (:month, :targetMeters)
                        on conflict (user_id, month) do update set target_meters = excluded.target_meters
                        """)
                .param("month", month)
                .param("targetMeters", plan.targetDistance().meters())
                .update();

        jdbc.sql("delete from planned_workout where month = :month").param("month", month).update();
        List<PlannedWorkout> workouts = plan.workouts();
        MapSqlParameterSource[] rows = IntStream.range(0, workouts.size())
                .mapToObj(position -> {
                    PlannedWorkout workout = workouts.get(position);
                    return new MapSqlParameterSource()
                            .addValue("month", month)
                            .addValue("position", position)
                            .addValue("workoutDate", workout.date())
                            .addValue("type", workout.type().name())
                            .addValue("distanceMeters", workout.distance().meters())
                            .addValue("note", workout.note());
                })
                .toArray(MapSqlParameterSource[]::new);
        batch.batchUpdate("""
                insert into planned_workout (month, position, workout_date, type, distance_meters, note)
                values (:month, :position, :workoutDate, :type, :distanceMeters, :note)
                """, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MonthlyPlan> findByMonth(YearMonth month) {
        Optional<Long> targetMeters = jdbc.sql("select target_meters from monthly_plan where month = :month")
                .param("month", month.toString())
                .query(Long.class)
                .optional();
        return targetMeters.map(target -> new MonthlyPlan(month, Distance.ofMeters(target), workouts(month)));
    }

    private List<PlannedWorkout> workouts(YearMonth month) {
        return jdbc.sql("""
                        select workout_date, type, distance_meters, note
                        from planned_workout where month = :month order by position
                        """)
                .param("month", month.toString())
                .query((rs, rowNum) -> new PlannedWorkout(
                        rs.getObject("workout_date", LocalDate.class),
                        WorkoutType.valueOf(rs.getString("type")),
                        Distance.ofMeters(rs.getLong("distance_meters")),
                        rs.getString("note")))
                .list();
    }
}
