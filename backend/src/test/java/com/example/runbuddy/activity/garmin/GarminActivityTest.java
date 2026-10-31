package com.example.runbuddy.activity.garmin;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.garmin.GarminActivity.ActivityType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class GarminActivityTest {

    private static final ActivityType RUNNING = new ActivityType("running", 1L, 17L);

    @Test
    void runningIsARun() {
        assertThat(activity(RUNNING, 5000.0, 1800.0).isRun()).isTrue();
    }

    /** Subtypes name running's typeId as their parent (from the API's own description of parentTypeId). */
    @Test
    void aSubtypeOfRunningIsARun() {
        assertThat(activity(new ActivityType("trail_running", 6L, 1L), 5000.0, 1800.0).isRun()).isTrue();
    }

    @Test
    void otherSportsAreNotRuns() {
        assertThat(activity(new ActivityType("cycling", 2L, 17L), 20000.0, 3600.0).toRunResult()).isEmpty();
        assertThat(activity(new ActivityType("road_biking", 10L, 2L), 20000.0, 3600.0).toRunResult()).isEmpty();
        assertThat(activity(null, 5000.0, 1800.0).toRunResult()).isEmpty();
    }

    @Test
    void aRunWithoutDistanceOrTimeIsSkipped() {
        assertThat(activity(RUNNING, 0.0, 0.0).toRunResult()).isEmpty();
        assertThat(activity(RUNNING, 0.4, 120.0).toRunResult()).isEmpty();
        assertThat(activity(RUNNING, 5000.0, 0.0).toRunResult()).isEmpty();
        assertThat(activity(RUNNING, null, 1800.0).toRunResult()).isEmpty();
    }

    @Test
    void theStartIsTheLocalWallClockTime() {
        GarminActivity activity = new GarminActivity(42L, "2026-10-31 09:40:33", RUNNING, 10017.8, 4651.95, 129.4);

        assertThat(activity.toRunResult()).hasValueSatisfying(run -> {
            assertThat(run.activityId()).isEqualTo("garmin-42");
            assertThat(run.startedAt()).isEqualTo(LocalDateTime.of(2026, 10, 31, 9, 40, 33));
            assertThat(run.distance().meters()).isEqualTo(10018);
            assertThat(run.averageHeartRate()).isEqualTo(129);
        });
    }

    private static GarminActivity activity(ActivityType type, Double meters, Double seconds) {
        return new GarminActivity(1L, "2026-10-05 07:00:00", type, meters, seconds, null);
    }
}
