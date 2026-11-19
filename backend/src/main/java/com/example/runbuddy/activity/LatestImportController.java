package com.example.runbuddy.activity;

import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Imports what is new since the latest stored run, whatever month that falls in (issue #12). */
@RestController
public class LatestImportController {

    private final RunImportService runImportService;
    private final NewRunChecker newRunChecker;

    public LatestImportController(RunImportService runImportService, NewRunChecker newRunChecker) {
        this.runImportService = runImportService;
        this.newRunChecker = newRunChecker;
    }

    @PostMapping("/api/runs/latest/import")
    public RunImportService.LatestImportResult importLatest() {
        return runImportService.importLatest();
    }

    /**
     * Whether there are runs to import (#40); stores nothing. The source is asked at most once per
     * {@code checkIntervalSeconds}, which is also when the screen should ask again.
     */
    @GetMapping("/api/runs/latest/check")
    public NewRunsBody check() {
        NewRunChecker.Checked checked = newRunChecker.check();
        return new NewRunsBody(checked.newRuns(), checked.checkedAt(), newRunChecker.interval().toSeconds());
    }

    /** @param newRuns newest first */
    public record NewRunsBody(List<NewRun> newRuns, Instant checkedAt, long checkIntervalSeconds) {
    }
}
