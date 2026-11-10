package com.example.runbuddy.review;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunResultRepository;

class InMemoryRunReviewRepositoryTest implements RunReviewRepositoryContract {

    private final RunReviewRepository reviews = new InMemoryRunReviewRepository();
    private final RunResultRepository runs = new InMemoryRunResultRepository();

    @Override
    public RunReviewRepository reviews() {
        return reviews;
    }

    @Override
    public RunResultRepository runs() {
        return runs;
    }
}
