package com.example.runbuddy.activity;

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

    public RunImportService(ActivitySource activitySource, RunResultRepository runResultRepository) {
        this.activitySource = activitySource;
        this.runResultRepository = runResultRepository;
    }

    /** Pulls the month's runs from the source. Safe to call repeatedly. */
    public ImportResult importRuns(YearMonth month) {
        String source = activitySource.getClass().getSimpleName();
        log.info("Import {}: fetching runs from {}", month, source);
        long started = System.nanoTime();
        List<RunResult> fetched;
        try {
            fetched = activitySource.fetchRuns(month);
        } catch (ActivitySourceUnavailableException e) {
            log.warn("Import {}: {} failed ({}): {}", month, source, e.reason(), e.getMessage());
            throw e;
        }
        int added = runResultRepository.saveAll(fetched);
        log.info("Import {}: {} runs from {}, {} new, {} already stored ({} ms)", month, fetched.size(), source,
                added, fetched.size() - added, (System.nanoTime() - started) / 1_000_000);
        return new ImportResult(month, fetched.size(), added);
    }

    public record ImportResult(YearMonth month, int fetched, int added) {
    }
}
