package com.example.runbuddy.activity;

import com.example.runbuddy.shared.DateRange;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RunImportService {

    private static final Logger log = LoggerFactory.getLogger(RunImportService.class);

    private final ActivitySource activitySource;
    private final RunResultRepository runResultRepository;
    private final Clock clock;

    public RunImportService(ActivitySource activitySource, RunResultRepository runResultRepository, Clock clock) {
        this.activitySource = activitySource;
        this.runResultRepository = runResultRepository;
        this.clock = clock;
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
        LocalDate today = LocalDate.now(clock);
        LocalDate from = runResultRepository.latestStart()
                .map(latest -> latest.toLocalDate().minusDays(1))
                .orElse(today.withDayOfMonth(1));
        // A run stored with a local date after ours (recorded abroad) must not push the start past today.
        DateRange days = new DateRange(from.isAfter(today) ? today : from, today);
        Counts counts = importDays(days);
        return new LatestImportResult(days.first(), days.last(), counts.fetched(), counts.added());
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
        int added = runResultRepository.saveAll(fetched);
        log.info("Import {}: {} runs from {}, {} new, {} already stored ({} ms)", days, fetched.size(), source,
                added, fetched.size() - added, (System.nanoTime() - started) / 1_000_000);
        return new Counts(fetched.size(), added);
    }

    private record Counts(int fetched, int added) {
    }

    public record ImportResult(YearMonth month, int fetched, int added) {
    }

    /** The days looked at (both included) and what was found there. */
    public record LatestImportResult(LocalDate from, LocalDate to, int fetched, int added) {
    }
}
