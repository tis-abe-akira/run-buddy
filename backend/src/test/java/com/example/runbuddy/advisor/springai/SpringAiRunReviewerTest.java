package com.example.runbuddy.advisor.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.Lap;
import com.example.runbuddy.activity.RunMetrics;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import com.example.runbuddy.review.RunReviewRequest;
import com.example.runbuddy.review.RunReviewer;
import com.example.runbuddy.review.SimilarRuns;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.ChatOptions;

class SpringAiRunReviewerTest {

    private static final RunResult RUN = new RunResult("r1", LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(10),
            Duration.ofMinutes(60), 150, new RunMetrics(168, 172, 180, 105, 30));
    private static final RunResult EARLIER = new RunResult("r0", LocalDateTime.of(2026, 10, 30, 6, 0), Distance.ofKilometers(10),
            Duration.ofMinutes(62), 155);
    private static final Lap LAP = new Lap(1, Duration.ZERO, Distance.ofKilometers(1), Duration.ofSeconds(355), 148, 156, 174, 180, 4);
    private static final RunReviewRequest REQUEST =
            new RunReviewRequest(RUN, List.of(LAP), SimilarRuns.forRun(RUN, List.of(EARLIER)));

    @Test
    void sendsTheRunItsLapsAndTheComparisonAndReadsTheThreeParts() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"headline":"安定した10km","runItself":"前半から一定のペース","comparison":"先週より心拍が5低い"}""");

        RunReviewer.Written written = reviewer(model).review(REQUEST);

        assertThat(written).isEqualTo(new RunReviewer.Written("安定した10km", "前半から一定のペース", "先週より心拍が5低い"));
        var prompt = model.prompts.getFirst();
        assertThat(prompt.getInstructions()).extracting(m -> m.getMessageType())
                .containsExactly(MessageType.SYSTEM, MessageType.USER);
        assertThat(prompt.getInstructions().getFirst().getText()).contains("Japanese").contains("headline");
        assertThat(prompt.getInstructions().get(1).getText())
                .contains("2026-11-02 06:00", "10.00 km", "pace 6:00 /km", "average heart rate 150 bpm", "max 168 bpm")
                .contains("1. 1.00 km in 5:55 (5:55 /km), heart rate 148/156 bpm, cadence 174 spm, climb 4 m")
                .contains("2026-10-30: 10.00 km, pace 6:12 /km, 155 bpm")
                .contains("12 s/km faster", "5 bpm lower")
                .contains("Days since the previous run: 3")
                .doesNotContainIgnoringCase("latitude");
    }

    @Test
    void saysSoWhenThereIsNothingToCompareWith() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"headline":"h","runItself":"a","comparison":"c"}""");

        reviewer(model).review(new RunReviewRequest(RUN, List.of(), SimilarRuns.forRun(RUN, List.of())));

        assertThat(model.prompts.getFirst().getInstructions().get(1).getText())
                .contains("Laps: not available")
                .contains("Similar runs: none in the last 28 days");
    }

    @Test
    void anAnswerMissingAPartIsNotAReview() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"headline":" ","runItself":"a","comparison":"c"}""");

        assertThatThrownBy(() -> reviewer(model).review(REQUEST))
                .isInstanceOf(AdvisorUnavailableException.class)
                .hasMessageContaining("incomplete");
    }

    @Test
    void aFailingModelIsUnavailable() {
        assertThatThrownBy(() -> reviewer(new ScriptedChatModel()).review(REQUEST))
                .isInstanceOf(AdvisorUnavailableException.class);
    }

    private static SpringAiRunReviewer reviewer(ScriptedChatModel model) {
        return new SpringAiRunReviewer("gemini", "flash", model, m -> ChatOptions.builder().model(m), e -> false);
    }
}
