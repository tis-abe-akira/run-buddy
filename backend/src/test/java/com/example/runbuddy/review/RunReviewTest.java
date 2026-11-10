package com.example.runbuddy.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.TrackPoint;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RunReviewTest {

    private static final Instant AT = Instant.parse("2026-11-30T00:00:00Z");

    @Test
    void keepsTheThreePartsTrimmed() {
        RunReview review = new RunReview("r1", "  よく走れました ", " 前半を抑えて後半に上げた ", " 先週より心拍が低い ", "gemini", "flash", AT);

        assertThat(review.headline()).isEqualTo("よく走れました");
        assertThat(review.runItself()).isEqualTo("前半を抑えて後半に上げた");
        assertThat(review.comparison()).isEqualTo("先週より心拍が低い");
    }

    @Test
    void everyPartMustSaySomething() {
        assertThatThrownBy(() -> new RunReview("r1", " ", "a", "c", "gemini", "flash", AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("headline");
        assertThatThrownBy(() -> new RunReview("r1", "h", "", "c", "gemini", "flash", AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("runItself");
        assertThatThrownBy(() -> new RunReview("r1", "h", "a", null, "gemini", "flash", AT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aModelThatRunsOnIsCutShort() {
        String longText = "あ".repeat(RunReview.MAX_SECTION_LENGTH + 50);

        RunReview review = new RunReview("r1", "あ".repeat(RunReview.MAX_HEADLINE_LENGTH + 5), longText, longText, "gemini", "flash", AT);

        assertThat(review.headline()).hasSize(RunReview.MAX_HEADLINE_LENGTH);
        assertThat(review.runItself()).hasSize(RunReview.MAX_SECTION_LENGTH);
        assertThat(review.comparison()).hasSize(RunReview.MAX_SECTION_LENGTH);
    }

    /** Positions locate the runner, so what goes to the AI has no place for them (ADR-0012). */
    @Test
    void theRequestToTheModelCannotCarryPositions() {
        assertThat(Arrays.stream(RunReviewRequest.class.getRecordComponents()).map(c -> c.getGenericType().getTypeName()))
                .noneMatch(type -> type.contains(TrackPoint.class.getSimpleName()) || type.contains("RunDetail"));
    }
}
