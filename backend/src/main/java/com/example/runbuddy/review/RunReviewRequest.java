package com.example.runbuddy.review;

import com.example.runbuddy.activity.Lap;
import com.example.runbuddy.activity.RunResult;
import java.util.List;
import java.util.Objects;

/**
 * What a {@link RunReviewer} is given: the run, its laps when its activity file is already stored, and
 * the similar recent runs the app chose. There is deliberately no field for the track: positions locate
 * the runner and never go to the AI (ADR-0012).
 *
 * @param laps empty when the run's file has not been downloaded; a review never downloads it
 */
public record RunReviewRequest(RunResult run, List<Lap> laps, SimilarRuns similar) {

    public RunReviewRequest {
        Objects.requireNonNull(run, "run");
        laps = List.copyOf(laps);
        Objects.requireNonNull(similar, "similar");
        if (!similar.run().equals(run)) {
            throw new IllegalArgumentException("the comparison is for another run: " + similar.run().activityId());
        }
    }
}
