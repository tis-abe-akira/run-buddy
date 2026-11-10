package com.example.runbuddy.review;

import java.time.Instant;
import java.util.Objects;

/**
 * An AI's review of one run (#15, #35): a headline, the run itself (viewpoint A) and how it compares
 * with similar recent runs (viewpoint C). Model output is untrusted, so each part must say something and
 * is cut to a length the page can show.
 *
 * @param reviewer   the provider that wrote it (e.g. "gemini", "fake")
 * @param model      the provider's model
 * @param reviewedAt when it was written
 */
public record RunReview(
        String activityId,
        String headline,
        String runItself,
        String comparison,
        String reviewer,
        String model,
        Instant reviewedAt) {

    public static final int MAX_HEADLINE_LENGTH = 80;
    public static final int MAX_SECTION_LENGTH = 800;

    public RunReview {
        Objects.requireNonNull(activityId, "activityId");
        Objects.requireNonNull(reviewer, "reviewer");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(reviewedAt, "reviewedAt");
        headline = part("headline", headline, MAX_HEADLINE_LENGTH);
        runItself = part("runItself", runItself, MAX_SECTION_LENGTH);
        comparison = part("comparison", comparison, MAX_SECTION_LENGTH);
    }

    private static String part(String name, String text, int maxLength) {
        Objects.requireNonNull(text, name);
        String stripped = text.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException("the " + name + " of a review must say something");
        }
        return stripped.length() <= maxLength ? stripped : stripped.substring(0, maxLength);
    }
}
