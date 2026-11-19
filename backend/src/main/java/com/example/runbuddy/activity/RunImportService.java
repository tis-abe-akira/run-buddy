package com.example.runbuddy.activity;

import com.example.runbuddy.shared.DateRange;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class RunImportService {

    private static final Logger log = LoggerFactory.getLogger(RunImportService.class);

    private final ActivitySource activitySource;
    private final RunResultRepository runResultRepository;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    /** @param events receives {@link RunsImported} when an import added runs */
    public RunImportService(ActivitySource activitySource, RunResultRepository runResultRepository, Clock clock,
            ApplicationEventPublisher events) {
        this.activitySource = activitySource;
        this.runResultRepository = runResultRepository;
        this.clock = clock;
        this.events = events;
    }

    /** Pulls the month's runs from the source again, e.g. after a run was edited there. Safe to repeat. */
    public ImportResult importRuns(YearMonth month) {
        Counts counts = importDays(DateRange.of(month));
        return new ImportResult(month, counts.fetched(), counts.added());
    }

    /**
     * Pulls what is new since the latest stored run: from the day before it (a run near midnight or
     * in another time zone can land a day either side) up to today. With nothing stored yet, from the
     * first of this month. Safe to repeat.
     */
    public LatestImportResult importLatest() {
        DateRange days = sinceLatest();
        Counts counts = importDays(days);
        return new LatestImportResult(days.first(), days.last(), counts.fetched(), counts.added(),
                counts.newRuns().stream().sorted(Comparator.comparing(RunResult::startedAt)).map(NewRun::of).toList());
    }

    /**
     * Which runs {@link #importLatest()} would add now (#40): the same days are asked for, but nothing is
     * stored and nothing is announced. Newest first.
     *
     * @throws ActivitySourceUnavailableException when the source cannot be reached
     */
    public List<NewRun> findNewRuns() {
        DateRange days = sinceLatest();
        Map<String, RunResult> byId = new LinkedHashMap<>();
        activitySource.fetchRuns(days).forEach(run -> byId.put(run.activityId(), run));
        List<NewRun> found = byId.values().stream()
                .filter(run -> runResultRepository.findById(run.activityId()).isEmpty())
                .sorted(Comparator.comparing(RunResult::startedAt).reversed())
                .map(NewRun::of)
                .toList();
        log.debug("Check {}: {} new runs", days, found.size());
        return found;
    }

    /**
     * From the day before the latest stored run (a run near midnight or in another time zone can land a
     * day either side) up to today; with nothing stored yet, from the first of this month.
     */
    private DateRange sinceLatest() {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = runResultRepository.latestStart()
                .map(latest -> latest.toLocalDate().minusDays(1))
                .orElse(today.withDayOfMonth(1));
        // A run stored with a local date after ours (recorded abroad) must not push the start past today.
        return new DateRange(from.isAfter(today) ? today : from, today);
    }

    private Counts importDays(DateRange days) {
        String source = activitySource.getClass().getSimpleName();
        log.info("Import {}: fetching runs from {}", days, source);
        long started = System.nanoTime();
        List<RunResult> fetched;
        try {
            fetched = activitySource.fetchRuns(days);
        } catch (ActivitySourceUnavailableException e) {
            log.warn("Import {}: {} failed ({}): {}", days, source, e.reason(), e.getMessage());
            throw e;
        }
        List<RunResult> added = runResultRepository.saveAll(fetched);
        log.info("Import {}: {} runs from {}, {} new, {} already stored ({} ms)", days, fetched.size(), source,
                added.size(), fetched.size() - added.size(), (System.nanoTime() - started) / 1_000_000);
        if (!added.isEmpty()) {
            events.publishEvent(new RunsImported(added));
        }
        return new Counts(fetched.size(), added);
    }

    private record Counts(int fetched, List<RunResult> newRuns) {

        int added() {
            return newRuns.size();
        }
    }

    public record ImportResult(YearMonth month, int fetched, int added) {
    }

    /**
     * The days looked at (both included) and what was found there.
     *
     * @param newRuns the runs stored for the first time, oldest first
     */
    public record LatestImportResult(LocalDate from, LocalDate to, int fetched, int added, List<NewRun> newRuns) {
    }
}
