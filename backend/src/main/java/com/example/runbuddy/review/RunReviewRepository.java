package com.example.runbuddy.review;

import java.util.Optional;

/**
 * Port for saved run reviews (#35): one per run, replaced when the run is reviewed again, gone when the
 * run is. Implementations: in memory, jOOQ (persistence/jooq).
 */
public interface RunReviewRepository {

    /** Keeps the review of a stored run, replacing any earlier one. */
    void save(RunReview review);

    Optional<RunReview> find(String activityId);
}
