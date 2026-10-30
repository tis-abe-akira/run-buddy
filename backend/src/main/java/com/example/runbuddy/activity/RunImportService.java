package com.example.runbuddy.activity;

import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RunImportService {

    private final ActivitySource activitySource;
    private final RunResultRepository runResultRepository;

    public RunImportService(ActivitySource activitySource, RunResultRepository runResultRepository) {
        this.activitySource = activitySource;
        this.runResultRepository = runResultRepository;
    }

    /** Pulls the month's runs from the source. Safe to call repeatedly. */
    public ImportResult importRuns(YearMonth month) {
        List<RunResult> fetched = activitySource.fetchRuns(month);
        int added = runResultRepository.saveAll(fetched);
        return new ImportResult(month, fetched.size(), added);
    }

    public record ImportResult(YearMonth month, int fetched, int added) {
    }
}
