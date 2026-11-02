package com.example.runbuddy.activity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Imports what is new since the latest stored run, whatever month that falls in (issue #12). */
@RestController
public class LatestImportController {

    private final RunImportService runImportService;

    public LatestImportController(RunImportService runImportService) {
        this.runImportService = runImportService;
    }

    @PostMapping("/api/runs/latest/import")
    public RunImportService.LatestImportResult importLatest() {
        return runImportService.importLatest();
    }
}
