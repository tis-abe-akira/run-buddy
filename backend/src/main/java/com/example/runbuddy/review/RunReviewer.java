package com.example.runbuddy.review;

/**
 * Port to an AI that reviews one run (#35). One implementation per provider, built next to the advisor
 * of the same provider (advisor/springai, advisor/fake); Spring AI types stay there. Implementations are
 * called outside any database transaction and must not read the database themselves: everything they
 * may use is in the {@link RunReviewRequest}.
 */
public interface RunReviewer {

    /** The provider, as in the advisor list (e.g. "gemini", "fake"). */
    String id();

    String model();

    /**
     * @throws com.example.runbuddy.advisor.AdvisorUnavailableException when the provider fails or refuses
     */
    Written review(RunReviewRequest request);

    /** The model's text, before {@link RunReview} checks and trims it. */
    record Written(String headline, String runItself, String comparison) {
    }
}
