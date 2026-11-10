package com.example.runbuddy.advisor.fake;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.review.RunReviewRequest;
import com.example.runbuddy.review.RunReviewer;
import com.example.runbuddy.review.SimilarRuns;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FakeRunReviewerTest {

    private static final RunResult RUN = new RunResult("r1", LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(10), Duration.ofMinutes(60), 150);

    @Test
    void writesTheThreePartsFromTheNumbers() {
        RunResult slowerEarlier = new RunResult("r0", LocalDateTime.of(2026, 10, 30, 6, 0), Distance.ofKilometers(10), Duration.ofMinutes(62), 155);

        RunReviewer.Written written = new FakeRunReviewer().review(new RunReviewRequest(RUN, List.of(), SimilarRuns.forRun(RUN, List.of(slowerEarlier))));

        assertThat(written.headline()).contains("10.0 km");
        assertThat(written.runItself()).contains("6:00/km", "150 bpm");
        assertThat(written.comparison()).contains("1 本", "12 秒速く", "5 拍低い");
    }

    @Test
    void writesAOneLinerFromTheNumbers() {
        String headline = new FakeRunReviewer().headline(new RunReviewRequest(RUN, List.of(), SimilarRuns.forRun(RUN, List.of())));

        assertThat(headline).contains("10.0 km", "6:00/km");
    }

    @Test
    void saysSoWhenThereIsNothingToCompareWith() {
        RunReviewer.Written written = new FakeRunReviewer().review(new RunReviewRequest(RUN, List.of(), SimilarRuns.forRun(RUN, List.of())));

        assertThat(written.comparison()).contains("比べられるランがありません");
    }
}
