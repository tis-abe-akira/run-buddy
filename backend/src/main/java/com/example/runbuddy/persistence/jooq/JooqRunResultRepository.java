package com.example.runbuddy.persistence.jooq;

import static com.example.runbuddy.persistence.jooq.generated.Tables.RUN_RESULT;
import static org.jooq.impl.DSL.excluded;

import com.example.runbuddy.activity.RunMetrics;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JooqRunResultRepository implements RunResultRepository {

    private final DSLContext dsl;

    public JooqRunResultRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public int saveAll(Collection<RunResult> runs) {
        // The last run wins when the same activity appears twice (PostgreSQL rejects an upsert
        // that touches one row twice).
        Map<String, RunResult> byId = new LinkedHashMap<>();
        runs.forEach(run -> byId.put(run.activityId(), run));
        if (byId.isEmpty()) {
            return 0;
        }

        Set<String> existing = dsl.select(RUN_RESULT.ACTIVITY_ID)
                .from(RUN_RESULT)
                .where(RUN_RESULT.ACTIVITY_ID.in(byId.keySet()))
                .fetchSet(RUN_RESULT.ACTIVITY_ID);

        var insert = dsl.insertInto(
                RUN_RESULT,
                RUN_RESULT.ACTIVITY_ID,
                RUN_RESULT.STARTED_AT,
                RUN_RESULT.DISTANCE_METERS,
                RUN_RESULT.DURATION_MILLIS,
                RUN_RESULT.AVERAGE_HEART_RATE,
                RUN_RESULT.MAX_HEART_RATE,
                RUN_RESULT.AVERAGE_CADENCE,
                RUN_RESULT.MAX_CADENCE,
                RUN_RESULT.AVERAGE_STRIDE_LENGTH_CM,
                RUN_RESULT.ELEVATION_GAIN_METERS);
        for (RunResult run : byId.values()) {
            RunMetrics metrics = run.metrics();
            insert = insert.values(
                    run.activityId(),
                    run.startedAt(),
                    run.distance().meters(),
                    run.duration().toMillis(),
                    run.averageHeartRate(),
                    metrics.maxHeartRate(),
                    metrics.averageCadence(),
                    metrics.maxCadence(),
                    metrics.averageStrideLengthCm(),
                    metrics.elevationGainMeters());
        }
        insert.onConflict(RUN_RESULT.USER_ID, RUN_RESULT.ACTIVITY_ID)
                .doUpdate()
                .set(RUN_RESULT.STARTED_AT, excluded(RUN_RESULT.STARTED_AT))
                .set(RUN_RESULT.DISTANCE_METERS, excluded(RUN_RESULT.DISTANCE_METERS))
                .set(RUN_RESULT.DURATION_MILLIS, excluded(RUN_RESULT.DURATION_MILLIS))
                .set(RUN_RESULT.AVERAGE_HEART_RATE, excluded(RUN_RESULT.AVERAGE_HEART_RATE))
                .set(RUN_RESULT.MAX_HEART_RATE, excluded(RUN_RESULT.MAX_HEART_RATE))
                .set(RUN_RESULT.AVERAGE_CADENCE, excluded(RUN_RESULT.AVERAGE_CADENCE))
                .set(RUN_RESULT.MAX_CADENCE, excluded(RUN_RESULT.MAX_CADENCE))
                .set(RUN_RESULT.AVERAGE_STRIDE_LENGTH_CM, excluded(RUN_RESULT.AVERAGE_STRIDE_LENGTH_CM))
                .set(RUN_RESULT.ELEVATION_GAIN_METERS, excluded(RUN_RESULT.ELEVATION_GAIN_METERS))
                .execute();

        return byId.size() - existing.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RunResult> findByMonth(YearMonth month) {
        return dsl.selectFrom(RUN_RESULT)
                .where(RUN_RESULT.STARTED_AT.ge(month.atDay(1).atStartOfDay()))
                .and(RUN_RESULT.STARTED_AT.lt(month.plusMonths(1).atDay(1).atStartOfDay()))
                .orderBy(RUN_RESULT.STARTED_AT)
                .fetch(row -> new RunResult(
                        row.getActivityId(),
                        row.getStartedAt(),
                        Distance.ofMeters(row.getDistanceMeters()),
                        Duration.ofMillis(row.getDurationMillis()),
                        row.getAverageHeartRate(),
                        new RunMetrics(
                                row.getMaxHeartRate(),
                                row.getAverageCadence(),
                                row.getMaxCadence(),
                                row.getAverageStrideLengthCm(),
                                row.getElevationGainMeters())));
    }
}
