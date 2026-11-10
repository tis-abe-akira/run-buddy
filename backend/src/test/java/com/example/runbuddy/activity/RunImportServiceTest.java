package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.shared.DateRange;
import com.example.runbuddy.shared.Distance;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RunImportServiceTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    private final RunResultRepository repository = new InMemoryRunResultRepository();

    /** Today is 2 Nov 2026. */
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-02T03:00:00Z"), ZoneOffset.UTC);

    /** Remembers the days it was asked for and answers with the given runs that fall in them. */
    private final List<DateRange> asked = new ArrayList<>();

    /** Events the service published. */
    private final List<Object> published = new ArrayList<>();

    private ActivitySource sourceOf(RunResult... runs) {
        return days -> {
            asked.add(days);
            return Stream.of(runs).filter(run -> days.contains(run.date())).toList();
        };
    }

    @Test
    void importStoresFetchedRuns() {
        ActivitySource source = month -> List.of(run("r1", 3), run("r2", 5));

        var result = new RunImportService(source, repository, clock, published::add).importRuns(NOVEMBER);

        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.added()).isEqualTo(2);
        assertThat(repository.findByMonth(NOVEMBER)).extracting(RunResult::activityId).containsExactly("r1", "r2");
    }

    @Test
    void importingTwiceDoesNotDuplicateRuns() {
        ActivitySource source = month -> List.of(run("r1", 3));
        RunImportService service = new RunImportService(source, repository, clock, published::add);

        service.importRuns(NOVEMBER);
        var second = service.importRuns(NOVEMBER);

        assertThat(second.added()).isZero();
        assertThat(repository.findByMonth(NOVEMBER)).hasSize(1);
    }

    @Test
    void theLatestImportStartsTheDayBeforeTheLatestStoredRun() {
        repository.saveAll(List.of(run("r1", 3)));
        RunImportService service = new RunImportService(sourceOf(run("r1", 3), run("r2", 5)), repository,
                Clock.fixed(Instant.parse("2026-11-06T03:00:00Z"), ZoneOffset.UTC), published::add);

        var result = service.importLatest();

        assertThat(asked).containsExactly(new DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6)));
        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(result.fetched()).isEqualTo(2);
        // The run seen again is updated, not added.
        assertThat(result.added()).isEqualTo(1);
        assertThat(repository.findByMonth(NOVEMBER)).extracting(RunResult::activityId).containsExactly("r1", "r2");
    }

    /** Only the runs not stored before are announced, e.g. for the automatic one-liner (#36). */
    @Test
    void announcesOnlyTheNewRuns() {
        repository.saveAll(List.of(run("r1", 3)));
        RunImportService service = new RunImportService(sourceOf(run("r1", 3), run("r2", 5)), repository,
                Clock.fixed(Instant.parse("2026-11-06T03:00:00Z"), ZoneOffset.UTC), published::add);

        service.importLatest();

        assertThat(published).containsExactly(new RunsImported(List.of(run("r2", 5))));
    }

    @Test
    void anImportThatAddsNothingAnnouncesNothing() {
        repository.saveAll(List.of(run("r1", 3)));

        new RunImportService(month -> List.of(run("r1", 3)), repository, clock, published::add).importRuns(NOVEMBER);

        assertThat(published).isEmpty();
    }

    @Test
    void theLatestImportCrossesMonths() {
        repository.saveAll(List.of(new RunResult("oct", LocalDateTime.of(2026, 10, 30, 7, 0),
                Distance.ofKilometers(8), Duration.ofMinutes(48), 150)));

        new RunImportService(sourceOf(), repository, clock, published::add).importLatest();

        assertThat(asked).containsExactly(new DateRange(LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 2)));
    }

    @Test
    void withNothingStoredTheLatestImportStartsAtTheFirstOfThisMonth() {
        new RunImportService(sourceOf(), repository, clock, published::add).importLatest();

        assertThat(asked).containsExactly(new DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)));
    }

    @Test
    void aStoredRunDatedAfterTodayStillAsksForToday() {
        // A run recorded abroad can be "tomorrow" in local time; the range must not start after it ends.
        repository.saveAll(List.of(run("ahead", 4)));

        new RunImportService(sourceOf(), repository, clock, published::add).importLatest();

        assertThat(asked).containsExactly(new DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2)));
    }

    private static RunResult run(String id, int day) {
        return new RunResult(
                id, LocalDateTime.of(2026, 11, day, 7, 0), Distance.ofKilometers(8), Duration.ofMinutes(48), 150);
    }
}
