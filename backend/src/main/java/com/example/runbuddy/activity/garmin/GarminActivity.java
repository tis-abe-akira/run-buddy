package com.example.runbuddy.activity.garmin;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.shared.Distance;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * The fields we use from one entry of Garmin's activity search ({@code SummarizedActivityDTO} in the
 * Personal API's OpenAPI document). Everything else in the response, including location and the
 * owner's name, is ignored. See ADR-0009.
 *
 * @param startTimeLocal "yyyy-MM-dd HH:mm:ss" in the time zone where the activity was recorded, no offset
 * @param distance       meters
 * @param duration       timer duration in seconds (pauses excluded), what Garmin Connect shows as "Time"
 * @param averageHR      beats per minute, absent for manual entries and runs without a heart rate sensor
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record GarminActivity(
        Long activityId,
        String startTimeLocal,
        ActivityType activityType,
        Double distance,
        Double duration,
        Double averageHR) {

    /** Garmin's key for running; its subtypes (trail_running, ...) name it as their parent. */
    static final String RUNNING = "running";

    /** typeId of {@link #RUNNING}, as returned by the API (observed 2026-10-31). */
    static final long RUNNING_TYPE_ID = 1;

    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** @param parentTypeId the parent's typeId; 17 means this type is a parent itself */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ActivityType(String typeKey, Long typeId, Long parentTypeId) {
    }

    boolean isRun() {
        return activityType != null
                && (RUNNING.equals(activityType.typeKey())
                        || Long.valueOf(RUNNING_TYPE_ID).equals(activityType.parentTypeId()));
    }

    /**
     * The run, or empty when this is not a run or records no distance or time (Garmin keeps manual
     * entries of 0 m in 0 s, which {@link RunResult} rejects).
     */
    Optional<RunResult> toRunResult() {
        if (!isRun() || activityId == null || startTimeLocal == null || distance == null || duration == null) {
            return Optional.empty();
        }
        Distance meters = Distance.ofMeters(Math.round(Math.max(distance, 0)));
        Duration time = Duration.ofMillis(Math.round(Math.max(duration, 0) * 1000));
        if (meters.isZero() || time.isZero()) {
            return Optional.empty();
        }
        Integer heartRate = averageHR == null ? null : (int) Math.round(averageHR);
        return Optional.of(new RunResult(
                "garmin-" + activityId, startedAt(), meters, time, heartRate));
    }

    /** Wall-clock time where the run happened, which is what the dashboard's days are made of. */
    LocalDateTime startedAt() {
        return LocalDateTime.parse(startTimeLocal, LOCAL_TIME);
    }
}
