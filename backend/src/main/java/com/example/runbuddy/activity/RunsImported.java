package com.example.runbuddy.activity;

import java.util.List;
import java.util.Objects;

/**
 * Published after an import stored runs it had not stored before (#36). Only the new ones: a run seen
 * again is updated, not announced. Listeners run after the runs are saved, outside any transaction.
 */
public record RunsImported(List<RunResult> added) {

    public RunsImported {
        added = List.copyOf(Objects.requireNonNull(added, "added"));
    }
}
