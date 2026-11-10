package com.example.runbuddy.activity;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RunResultRepository {

    /**
     * Stores the runs, replacing any with the same activity id.
     *
     * @return the runs that were not stored before, in the order given (the last one when an activity id
     *         appears twice)
     */
    List<RunResult> saveAll(Collection<RunResult> runs);

    /** Runs started in the month, oldest first. */
    List<RunResult> findByMonth(YearMonth month);

    Optional<RunResult> findById(String activityId);

    /** When the most recent stored run started; empty when nothing is stored. */
    Optional<LocalDateTime> latestStart();
}
