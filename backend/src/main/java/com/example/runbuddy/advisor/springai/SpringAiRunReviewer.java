package com.example.runbuddy.advisor.springai;

import com.example.runbuddy.activity.Lap;
import com.example.runbuddy.activity.RunMetrics;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import com.example.runbuddy.review.RunReviewRequest;
import com.example.runbuddy.review.RunReviewer;
import com.example.runbuddy.review.SimilarRuns;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Reviews one run with a Spring AI chat model (#35, ADR-0016): the app writes out the run, its laps and
 * the similar runs it chose, and the model answers with structured output. No tools: the model is given
 * everything it may use, which keeps it to one request and keeps positions out (ADR-0012).
 */
public class SpringAiRunReviewer implements RunReviewer {

    private static final Logger log = LoggerFactory.getLogger(SpringAiRunReviewer.class);

    private static final String SYSTEM_PROMPT = """
            You are a running coach reviewing one run for the runner who ran it.
            Write in Japanese, plain text without Markdown, short and concrete.
            Use only the numbers given; quote them as given and do not invent measurements.

            Answer with three parts:
            - headline: one sentence that sums up the run (at most 40 characters).
            - runItself: the run on its own: pace and heart rate together, how the laps went (an even pace,
              a fade or a strong finish), cadence and climb when they matter. A few sentences.
            - comparison: the run against the similar recent runs given: faster or slower, heart rate higher
              or lower for the pace, and the rest days before it. If there are no similar runs, say so in one
              sentence instead of guessing.
            """;

    private static final DateTimeFormatter START = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String id;
    private final String model;
    private final ChatClient chatClient;
    private final Function<String, ChatOptions.Builder<?>> optionsForModel;
    private final Predicate<Throwable> isRateLimit;

    public SpringAiRunReviewer(String id, String model, ChatModel chatModel,
            Function<String, ChatOptions.Builder<?>> optionsForModel, Predicate<Throwable> isRateLimit) {
        this.id = id;
        this.model = model;
        this.chatClient = ChatClient.create(chatModel);
        this.optionsForModel = optionsForModel;
        this.isRateLimit = isRateLimit;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public Written review(RunReviewRequest request) {
        Answer answer;
        try {
            answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(describe(request))
                    .options(optionsForModel.apply(model))
                    .call()
                    .entity(Answer.class);
        } catch (RuntimeException e) {
            if (Stream.iterate((Throwable) e, Objects::nonNull, Throwable::getCause).limit(10).anyMatch(isRateLimit)) {
                log.warn("{} ({}) rate limit or quota exceeded while reviewing a run", id, model);
                throw new AdvisorUnavailableException(
                        AdvisorUnavailableException.Reason.RATE_LIMITED, id + " rate limit or quota exceeded", e);
            }
            log.warn("{} ({}) could not review the run", id, model, e);
            throw new AdvisorUnavailableException(id + " could not review the run: " + ProviderFailures.reason(e), e);
        }
        if (answer == null || blank(answer.headline()) || blank(answer.runItself()) || blank(answer.comparison())) {
            throw new AdvisorUnavailableException(id + " returned an incomplete review", null);
        }
        return new Written(answer.headline(), answer.runItself(), answer.comparison());
    }

    /** The model's structured output. */
    record Answer(String headline, String runItself, String comparison) {
    }

    /** The run, its laps and the comparison, as plain lines. Package-private for the test. */
    static String describe(RunReviewRequest request) {
        RunResult run = request.run();
        RunMetrics m = run.metrics();
        StringBuilder text = new StringBuilder();
        text.append("Run: ").append(run.startedAt().format(START)).append(", ").append(km(run.distance().toKilometers()))
                .append(" in ").append(time(run.duration())).append(", pace ").append(pace(run.pace()))
                .append(", average heart rate ").append(bpm(run.averageHeartRate()))
                .append(", max ").append(bpm(m.maxHeartRate()))
                .append(", cadence ").append(spm(m.averageCadence())).append(" (max ").append(spm(m.maxCadence())).append(")")
                .append(", stride ").append(m.averageStrideLengthCm() == null ? "not recorded" : m.averageStrideLengthCm() + " cm")
                .append(", climb ").append(m.elevationGainMeters() == null ? "not recorded" : m.elevationGainMeters() + " m")
                .append("\n\n");

        if (request.laps().isEmpty()) {
            text.append("Laps: not available\n\n");
        } else {
            text.append("Laps:\n");
            for (Lap lap : request.laps()) {
                text.append(lap.number()).append(". ").append(km(lap.distance().toKilometers()))
                        .append(" in ").append(time(lap.duration()))
                        .append(" (").append(lap.pace().map(SpringAiRunReviewer::pace).orElse("no pace")).append(")")
                        .append(", heart rate ").append(lap.averageHeartRate() == null ? "not recorded"
                                : lap.averageHeartRate() + "/" + (lap.maxHeartRate() == null ? "?" : lap.maxHeartRate()) + " bpm")
                        .append(", cadence ").append(spm(lap.averageCadence()))
                        .append(", climb ").append(lap.elevationGainMeters() == null ? "not recorded" : lap.elevationGainMeters() + " m")
                        .append("\n");
            }
            text.append("\n");
        }

        SimilarRuns similar = request.similar();
        if (similar.runs().isEmpty()) {
            text.append("Similar runs: none in the last 28 days\n");
        } else {
            text.append("Similar runs (last 28 days, distance within 25%), newest first:\n");
            text.append(similar.runs().stream()
                    .map(r -> "- " + r.date() + ": " + km(r.distance().toKilometers()) + ", pace " + pace(r.pace())
                            + ", " + bpm(r.averageHeartRate()))
                    .collect(Collectors.joining("\n"))).append("\n");
            text.append("Average of the similar runs: pace ").append(similar.averagePace().map(SpringAiRunReviewer::pace).orElse("-"))
                    .append(", heart rate ").append(similar.averageHeartRate().map(SpringAiRunReviewer::bpm).orElse("not recorded"))
                    .append(".\n");
            text.append("This run against them: ")
                    .append(similar.paceDifference().map(SpringAiRunReviewer::paceDifference).orElse("pace not comparable"))
                    .append(", ")
                    .append(similar.heartRateDifference().map(SpringAiRunReviewer::heartRateDifference).orElse("heart rate not comparable"))
                    .append(".\n");
        }
        text.append("Days since the previous run: ")
                .append(similar.daysSincePreviousRun().map(String::valueOf).orElse("no earlier run")).append("\n");
        return text.toString();
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }

    private static String km(double km) {
        return String.format(Locale.ROOT, "%.2f km", km);
    }

    private static String time(Duration duration) {
        long s = duration.toSeconds();
        return s >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                : String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private static String pace(Duration perKm) {
        long s = Math.round(perKm.toMillis() / 1000.0);
        return String.format(Locale.ROOT, "%d:%02d /km", s / 60, s % 60);
    }

    private static String bpm(Integer heartRate) {
        return heartRate == null ? "heart rate not recorded" : heartRate + " bpm";
    }

    private static String spm(Integer cadence) {
        return cadence == null ? "not recorded" : cadence + " spm";
    }

    private static String paceDifference(Duration difference) {
        long s = difference.toSeconds();
        return s == 0 ? "the same pace" : Math.abs(s) + " s/km " + (s < 0 ? "faster" : "slower");
    }

    private static String heartRateDifference(int difference) {
        return difference == 0 ? "the same heart rate" : Math.abs(difference) + " bpm " + (difference < 0 ? "lower" : "higher");
    }
}
