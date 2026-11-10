package com.example.runbuddy.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The run being reviewed is 10 km in 60 minutes (6:00 /km) at 150 bpm, on 30 Nov 2026 at 6:00. */
class SimilarRunsTest {

    private static final LocalDateTime REVIEWED_AT = LocalDateTime.of(2026, 11, 30, 6, 0);
    private static final RunResult REVIEWED = run("reviewed", REVIEWED_AT, 10, 60, 150);

    @Test
    void comparesWithRunsOfASimilarDistanceInTheFourWeeksBefore() {
        RunResult similar = run("similar", REVIEWED_AT.minusDays(3), 9, 57, 155);
        RunResult tooShort = run("short", REVIEWED_AT.minusDays(2), 5, 30, 140);
        RunResult tooLong = run("long", REVIEWED_AT.minusDays(4), 20, 130, 150);

        SimilarRuns comparison = SimilarRuns.forRun(REVIEWED, List.of(similar, tooShort, tooLong, REVIEWED));

        assertThat(comparison.runs()).containsExactly(similar);
    }

    @Test
    void theDistanceMayDifferByAQuarterEitherWay() {
        RunResult shortest = run("7.5", REVIEWED_AT.minusDays(1), 7.5, 45, null);
        RunResult longest = run("12.5", REVIEWED_AT.minusDays(2), 12.5, 75, null);
        RunResult justTooLong = run("12.6", REVIEWED_AT.minusDays(3), 12.6, 76, null);

        assertThat(SimilarRuns.forRun(REVIEWED, List.of(shortest, longest, justTooLong)).runs())
                .containsExactly(shortest, longest);
    }

    @Test
    void looksBackTwentyEightDaysAndNeverAhead() {
        RunResult oldestKept = run("28d", REVIEWED_AT.minusDays(28), 10, 60, null);
        RunResult tooOld = run("29d", REVIEWED_AT.minusDays(28).minusMinutes(1), 10, 60, null);
        RunResult later = run("later", REVIEWED_AT.plusHours(5), 10, 60, null);

        assertThat(SimilarRuns.forRun(REVIEWED, List.of(oldestKept, tooOld, later)).runs()).containsExactly(oldestKept);
    }

    @Test
    void keepsTheFiveMostRecent() {
        List<RunResult> six = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(day -> run("d" + day, REVIEWED_AT.minusDays(day), 10, 60, null))
                .toList();

        assertThat(SimilarRuns.forRun(REVIEWED, six).runs()).extracting(RunResult::activityId)
                .containsExactly("d1", "d2", "d3", "d4", "d5");
    }

    @Test
    void averagesPaceOverTheWholeDistanceAndHeartRateOverTheRunsThatRecordedIt() {
        RunResult a = run("a", REVIEWED_AT.minusDays(1), 10, 65, 160);
        RunResult b = run("b", REVIEWED_AT.minusDays(2), 10, 55, null);

        SimilarRuns comparison = SimilarRuns.forRun(REVIEWED, List.of(a, b));

        assertThat(comparison.averagePace()).hasValue(Duration.ofMinutes(6));
        assertThat(comparison.averageHeartRate()).hasValue(160);
        assertThat(comparison.paceDifference()).hasValue(Duration.ZERO);
        assertThat(comparison.heartRateDifference()).hasValue(-10);
    }

    @Test
    void nothingToCompareWithIsEmptyNotZero() {
        SimilarRuns comparison = SimilarRuns.forRun(REVIEWED, List.of(REVIEWED));

        assertThat(comparison.runs()).isEmpty();
        assertThat(comparison.averagePace()).isEmpty();
        assertThat(comparison.averageHeartRate()).isEmpty();
        assertThat(comparison.paceDifference()).isEmpty();
        assertThat(comparison.heartRateDifference()).isEmpty();
    }

    @Test
    void countsTheRestDaysSinceThePreviousRunOfAnyDistance() {
        RunResult shortRunTwoDaysBefore = run("short", REVIEWED_AT.minusDays(2), 3, 20, null);

        assertThat(SimilarRuns.forRun(REVIEWED, List.of(shortRunTwoDaysBefore)).daysSincePreviousRun()).hasValue(2L);
        assertThat(SimilarRuns.forRun(REVIEWED, List.of()).daysSincePreviousRun()).isEmpty();
    }

    private static RunResult run(String id, LocalDateTime start, double km, long minutes, Integer heartRate) {
        return new RunResult(id, start, Distance.ofKilometers(km), Duration.ofMinutes(minutes), heartRate);
    }
}
