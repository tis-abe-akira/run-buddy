package com.example.runbuddy.review;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The recent runs a run is compared with (viewpoint C of #15). The app chooses them, not the AI, so
 * the comparison is stable and tested: runs started in the {@value #LOOKBACK_DAYS} days before, whose
 * distance is within a quarter of the run's, newest first, at most {@value #MAX_RUNS}.
 *
 * @param run                  the run being reviewed
 * @param runs                 the similar runs, newest first
 * @param daysSincePreviousRun calendar days since the previous run of any distance; empty when there is none
 */
public record SimilarRuns(RunResult run, List<RunResult> runs, Optional<Long> daysSincePreviousRun) {

    static final int LOOKBACK_DAYS = 28;
    static final double DISTANCE_TOLERANCE = 0.25;
    static final int MAX_RUNS = 5;

    public SimilarRuns {
        Objects.requireNonNull(run, "run");
        runs = List.copyOf(runs);
        Objects.requireNonNull(daysSincePreviousRun, "daysSincePreviousRun");
    }

    /** Picks the runs to compare {@code run} with from {@code stored}, which may include the run itself. */
    public static SimilarRuns forRun(RunResult run, List<RunResult> stored) {
        LocalDateTime from = run.startedAt().minusDays(LOOKBACK_DAYS);
        List<RunResult> before = stored.stream()
                .filter(other -> !other.activityId().equals(run.activityId()))
                .filter(other -> other.startedAt().isBefore(run.startedAt()))
                .sorted(Comparator.comparing(RunResult::startedAt).reversed())
                .toList();
        List<RunResult> similar = before.stream()
                .filter(other -> !other.startedAt().isBefore(from))
                .filter(other -> closeInDistance(run.distance(), other.distance()))
                .limit(MAX_RUNS)
                .toList();
        Optional<Long> rest = before.stream().findFirst()
                .map(previous -> ChronoUnit.DAYS.between(previous.date(), run.date()));
        return new SimilarRuns(run, similar, rest);
    }

    /** Total time over total distance of the similar runs. */
    public Optional<Duration> averagePace() {
        if (runs.isEmpty()) {
            return Optional.empty();
        }
        Duration time = runs.stream().map(RunResult::duration).reduce(Duration.ZERO, Duration::plus);
        double km = Distance.sum(runs.stream().map(RunResult::distance).toList()).toKilometers();
        return Optional.of(Duration.ofSeconds(Math.round(time.toSeconds() / km)));
    }

    /** Mean of the average heart rates of the similar runs that recorded one. */
    public Optional<Integer> averageHeartRate() {
        OptionalDouble mean = runs.stream().flatMap(r -> r.heartRate().stream()).mapToInt(Integer::intValue).average();
        return mean.isPresent() ? Optional.of((int) Math.round(mean.getAsDouble())) : Optional.empty();
    }

    /** The run's pace minus the average: negative when it was faster. */
    public Optional<Duration> paceDifference() {
        return averagePace().map(average -> Duration.ofSeconds(run.pace().toSeconds()).minus(average));
    }

    /** The run's average heart rate minus the average: negative when it was lower. */
    public Optional<Integer> heartRateDifference() {
        return run.heartRate().flatMap(heartRate -> averageHeartRate().map(average -> heartRate - average));
    }

    private static boolean closeInDistance(Distance reviewed, Distance other) {
        return Math.abs(other.meters() - reviewed.meters()) <= reviewed.meters() * DISTANCE_TOLERANCE;
    }
}
