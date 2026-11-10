package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** What every {@link RunResultRepository} must do. See MonthlyPlanRepositoryContract. */
public interface RunResultRepositoryContract {

    YearMonth NOVEMBER = YearMonth.of(2026, 11);

    RunResultRepository repository();

    @Test
    default void runsAreFoundByMonthOldestFirst() {
        RunResult later = run("b", LocalDateTime.of(2026, 11, 20, 6, 0), 10);
        RunResult earlier = run("a", LocalDateTime.of(2026, 11, 2, 6, 0), 8);

        repository().saveAll(List.of(later, earlier));

        assertThat(repository().findByMonth(NOVEMBER)).containsExactly(earlier, later);
    }

    @Test
    default void monthBoundariesAreExact() {
        RunResult lastMomentOfOctober = run("oct", LocalDateTime.of(2026, 10, 31, 23, 59, 59), 5);
        RunResult firstMomentOfNovember = run("nov-first", LocalDateTime.of(2026, 11, 1, 0, 0), 5);
        RunResult lastMomentOfNovember = run("nov-last", LocalDateTime.of(2026, 11, 30, 23, 59, 59), 5);
        RunResult firstMomentOfDecember = run("dec", LocalDateTime.of(2026, 12, 1, 0, 0), 5);

        repository().saveAll(List.of(lastMomentOfOctober, firstMomentOfNovember, lastMomentOfNovember, firstMomentOfDecember));

        assertThat(repository().findByMonth(NOVEMBER)).containsExactly(firstMomentOfNovember, lastMomentOfNovember);
    }

    @Test
    default void newRunsAreReturned() {
        assertThat(repository().saveAll(List.of(run("a", 2), run("b", 3)))).containsExactly(run("a", 2), run("b", 3));
    }

    @Test
    default void savingTheSameRunAgainReplacesItWithoutReturningIt() {
        repository().saveAll(List.of(run("a", 2)));
        RunResult corrected = new RunResult(
                "a", LocalDateTime.of(2026, 11, 2, 7, 0), Distance.ofKilometers(9.5), Duration.ofMinutes(57), 151);

        List<RunResult> added = repository().saveAll(List.of(corrected, run("b", 3)));

        assertThat(added).containsExactly(run("b", 3));
        assertThat(repository().findByMonth(NOVEMBER)).containsExactly(corrected, run("b", 3));
    }

    @Test
    default void sameRunTwiceInOneBatchIsStoredOnce() {
        RunResult first = run("a", 2);
        RunResult second = new RunResult("a", first.startedAt(), Distance.ofKilometers(7), Duration.ofMinutes(40), null);

        List<RunResult> added = repository().saveAll(List.of(first, second));

        assertThat(added).containsExactly(second);
        assertThat(repository().findByMonth(NOVEMBER)).containsExactly(second);
    }

    @Test
    default void emptyBatchStoresNothing() {
        assertThat(repository().saveAll(List.of())).isEmpty();
        assertThat(repository().findByMonth(NOVEMBER)).isEmpty();
    }

    @Test
    default void missingHeartRateStaysMissing() {
        RunResult noHeartRate = new RunResult(
                "a", LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(5), Duration.ofMinutes(30), null);

        repository().saveAll(List.of(noHeartRate));

        assertThat(repository().findByMonth(NOVEMBER)).singleElement()
                .satisfies(run -> assertThat(run.heartRate()).isEmpty());
    }

    @Test
    default void metricsAreStoredAndReadBack() {
        RunMetrics metrics = new RunMetrics(181, 172, 188, 98, 42);
        RunResult measured = new RunResult(
                "a", LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(10), Duration.ofMinutes(55), 150,
                metrics);

        repository().saveAll(List.of(measured));

        assertThat(repository().findByMonth(NOVEMBER)).singleElement()
                .satisfies(run -> assertThat(run.metrics()).isEqualTo(metrics));
    }

    @Test
    default void missingMetricsStayMissing() {
        repository().saveAll(List.of(run("a", 2)));

        assertThat(repository().findByMonth(NOVEMBER)).singleElement()
                .satisfies(run -> assertThat(run.metrics()).isEqualTo(RunMetrics.NONE));
    }

    @Test
    default void savingAgainReplacesTheMetrics() {
        RunResult before = run("a", 2);
        RunMetrics metrics = new RunMetrics(181, 172, 188, 98, 0);
        RunResult after = new RunResult(
                before.activityId(), before.startedAt(), before.distance(), before.duration(),
                before.averageHeartRate(), metrics);

        repository().saveAll(List.of(before));
        repository().saveAll(List.of(after));

        assertThat(repository().findByMonth(NOVEMBER)).singleElement()
                .satisfies(run -> assertThat(run.metrics()).isEqualTo(metrics));
    }

    @Test
    default void theLatestStartIsTheMostRecentRunAcrossMonths() {
        repository().saveAll(List.of(
                run("oct", LocalDateTime.of(2026, 10, 31, 18, 0), 5),
                run("nov-early", LocalDateTime.of(2026, 11, 2, 6, 0), 5),
                run("nov-later", LocalDateTime.of(2026, 11, 2, 19, 30), 5)));

        assertThat(repository().latestStart()).contains(LocalDateTime.of(2026, 11, 2, 19, 30));
    }

    @Test
    default void withoutRunsThereIsNoLatestStart() {
        assertThat(repository().latestStart()).isEmpty();
    }

    @Test
    default void aRunIsFoundByItsId() {
        RunResult stored = run("a", 2);
        repository().saveAll(List.of(stored, run("b", 3)));

        assertThat(repository().findById("a")).contains(stored);
        assertThat(repository().findById("missing")).isEmpty();
    }

    private static RunResult run(String id, int day) {
        return run(id, LocalDateTime.of(2026, 11, day, 6, 30), 8);
    }

    private static RunResult run(String id, LocalDateTime startedAt, double km) {
        return new RunResult(id, startedAt, Distance.ofKilometers(km), Duration.ofSeconds(Math.round(km * 330)), 150);
    }
}
