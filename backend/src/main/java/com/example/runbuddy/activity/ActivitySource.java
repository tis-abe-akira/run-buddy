package com.example.runbuddy.activity;

import com.example.runbuddy.shared.DateRange;
import java.util.List;

/**
 * Port to an external service that records runs (Garmin, or a dummy in Phase 1). See ADR-0004.
 */
@FunctionalInterface
public interface ActivitySource {

    /** Runs whose local start date falls in the range, both ends included. */
    List<RunResult> fetchRuns(DateRange days);
}
