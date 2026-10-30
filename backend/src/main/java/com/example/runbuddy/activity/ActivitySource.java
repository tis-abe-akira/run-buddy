package com.example.runbuddy.activity;

import java.time.YearMonth;
import java.util.List;

/**
 * Port to an external service that records runs (Garmin, or a dummy in Phase 1). See ADR-0004.
 */
@FunctionalInterface
public interface ActivitySource {

    List<RunResult> fetchRuns(YearMonth month);
}
