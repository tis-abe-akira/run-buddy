package com.example.runbuddy.persistence.jdbc;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** Persistence shape of a {@link RunResult}. user_id is filled by the database (see MonthlyPlanRow). */
@Table("run_result")
record RunResultRow(
        @Id String activityId,
        LocalDateTime startedAt,
        long distanceMeters,
        long durationMillis,
        Integer averageHeartRate) {

    static RunResultRow from(RunResult run) {
        return new RunResultRow(
                run.activityId(),
                run.startedAt(),
                run.distance().meters(),
                run.duration().toMillis(),
                run.averageHeartRate());
    }

    RunResult toRun() {
        return new RunResult(
                activityId, startedAt, Distance.ofMeters(distanceMeters), Duration.ofMillis(durationMillis), averageHeartRate);
    }
}
