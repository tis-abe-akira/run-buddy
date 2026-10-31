package com.example.runbuddy.persistence.jdbcclient;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JdbcClientRunResultRepository implements RunResultRepository {

    private final JdbcClient jdbc;
    private final NamedParameterJdbcTemplate batch;

    public JdbcClientRunResultRepository(JdbcClient jdbc, NamedParameterJdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    @Override
    public int saveAll(Collection<RunResult> runs) {
        Map<String, RunResult> byId = new LinkedHashMap<>();
        runs.forEach(run -> byId.put(run.activityId(), run));
        if (byId.isEmpty()) {
            return 0;
        }

        Set<String> existing = Set.copyOf(jdbc.sql("select activity_id from run_result where activity_id in (:ids)")
                .param("ids", byId.keySet())
                .query(String.class)
                .list());

        MapSqlParameterSource[] rows = byId.values().stream()
                .map(run -> new MapSqlParameterSource()
                        .addValue("activityId", run.activityId())
                        .addValue("startedAt", run.startedAt())
                        .addValue("distanceMeters", run.distance().meters())
                        .addValue("durationMillis", run.duration().toMillis())
                        .addValue("averageHeartRate", run.averageHeartRate()))
                .toArray(MapSqlParameterSource[]::new);
        batch.batchUpdate("""
                insert into run_result (activity_id, started_at, distance_meters, duration_millis, average_heart_rate)
                values (:activityId, :startedAt, :distanceMeters, :durationMillis, :averageHeartRate)
                on conflict (user_id, activity_id) do update set
                    started_at = excluded.started_at,
                    distance_meters = excluded.distance_meters,
                    duration_millis = excluded.duration_millis,
                    average_heart_rate = excluded.average_heart_rate
                """, rows);

        return byId.size() - existing.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunResult> findByMonth(YearMonth month) {
        return jdbc.sql("""
                        select activity_id, started_at, distance_meters, duration_millis, average_heart_rate
                        from run_result
                        where started_at >= :from and started_at < :until
                        order by started_at
                        """)
                .param("from", month.atDay(1).atStartOfDay())
                .param("until", month.plusMonths(1).atDay(1).atStartOfDay())
                .query((rs, rowNum) -> new RunResult(
                        rs.getString("activity_id"),
                        rs.getObject("started_at", LocalDateTime.class),
                        Distance.ofMeters(rs.getLong("distance_meters")),
                        Duration.ofMillis(rs.getLong("duration_millis")),
                        rs.getObject("average_heart_rate", Integer.class)))
                .list();
    }
}
