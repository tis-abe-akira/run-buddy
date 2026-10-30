package com.example.runbuddy.activity;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;

public interface RunResultRepository {

    /**
     * Stores the runs, replacing any with the same activity id.
     *
     * @return how many of the runs were not stored before
     */
    int saveAll(Collection<RunResult> runs);

    /** Runs started in the month, oldest first. */
    List<RunResult> findByMonth(YearMonth month);
}
