package com.example.runbuddy.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/** What every {@link RunReviewRepository} must do (#35). A review belongs to a stored run. */
public interface RunReviewRepositoryContract {

    RunReviewRepository reviews();

    RunResultRepository runs();

    @Test
    default void aSavedReviewIsFoundByItsRun() {
        storeRun("garmin-1");
        RunReview review = review("garmin-1", "よく走れました");

        reviews().save(review);

        assertThat(reviews().find("garmin-1")).hasValue(review);
    }

    @Test
    default void aRunNotYetReviewedHasNone() {
        storeRun("garmin-1");

        assertThat(reviews().find("garmin-1")).isEmpty();
        assertThat(reviews().find("unknown")).isEmpty();
    }

    /** "Review again" replaces the review: a run has one at a time. */
    @Test
    default void reviewingAgainReplacesTheReview() {
        storeRun("garmin-1");
        reviews().save(review("garmin-1", "最初の評価"));

        RunReview again = review("garmin-1", "評価し直した");
        reviews().save(again);

        assertThat(reviews().find("garmin-1")).hasValue(again);
    }

    private void storeRun(String id) {
        runs().saveAll(List.of(new RunResult(id, LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(8),
                Duration.ofMinutes(45), 150)));
    }

    /** Microsecond precision, as PostgreSQL keeps it. */
    private static RunReview review(String activityId, String headline) {
        return new RunReview(activityId, headline, "前半を抑えて後半に上げた", "先週より心拍が低い", "gemini", "flash",
                Instant.parse("2026-11-02T07:00:00.123456Z").truncatedTo(ChronoUnit.MICROS));
    }
}
