package com.example.runbuddy.review;

import java.time.Instant;
import java.util.Objects;

/**
 * An AI's review of one run (#15, #35): a headline, the run itself (viewpoint A) and how it compares
 * with similar recent runs (viewpoint C). Model output is untrusted, so each part must say something and
 * is cut to a length the page can show.
 *
 * <p>A <em>brief</em> review (#36) is only the headline, written when the run is imported; it has neither
 * {@code runItself} nor {@code comparison} (both null) until the detailed review replaces it. A review
 * has both of them or neither.
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
        if (runItself != null || comparison != null) {
            if (runItself == null || comparison == null) {
                throw new NullPointerException("a detailed review needs both runItself and comparison");
            }
            runItself = part("runItself", runItself, MAX_SECTION_LENGTH);
            comparison = part("comparison", comparison, MAX_SECTION_LENGTH);
        }
    }

    /** Only the headline, as written on import (#36). */
    public static RunReview brief(String activityId, String headline, String reviewer, String model, Instant reviewedAt) {
        return new RunReview(activityId, headline, null, null, reviewer, model, reviewedAt);
    }

    /** True for the one-line review written on import; the detailed review has been asked for otherwise. */
    public boolean isBrief() {
        return runItself == null;
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
