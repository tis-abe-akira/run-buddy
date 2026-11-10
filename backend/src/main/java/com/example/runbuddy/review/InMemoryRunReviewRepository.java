package com.example.runbuddy.review;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory storage for the "memory" profile and for tests. Data is lost on restart. */
public class InMemoryRunReviewRepository implements RunReviewRepository {

    private final Map<String, RunReview> reviewsByRun = new ConcurrentHashMap<>();

    @Override
    public void save(RunReview review) {
        reviewsByRun.put(review.activityId(), review);
    }

    @Override
    public Optional<RunReview> find(String activityId) {
        return Optional.ofNullable(reviewsByRun.get(activityId));
    }
}
