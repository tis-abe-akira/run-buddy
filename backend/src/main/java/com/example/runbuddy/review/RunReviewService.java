package com.example.runbuddy.review;

import com.example.runbuddy.activity.ActivityFileReader;
import com.example.runbuddy.activity.Lap;
import com.example.runbuddy.activity.RunFileRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.activity.UnreadableActivityFileException;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reviews a run on request and keeps the review (#35, ADR-0016).
 *
 * <p>Not transactional: each repository call is its own short transaction, and the model is called
 * between them, never inside one (backend/CLAUDE.md). The activity file is used only when it is already
 * stored; a review never downloads it (ADR-0012).
 */
@Service
public class RunReviewService {

    private static final Logger log = LoggerFactory.getLogger(RunReviewService.class);

    private final RunResultRepository runs;
    private final RunFileRepository files;
    private final ActivityFileReader reader;
    private final RunReviewRepository reviews;
    private final List<RunReviewer> reviewers;
    private final Clock clock;
    private final Duration timeout;

    /** Model calls run here, so a call that takes too long can be abandoned (and interrupted). */
    private static final ExecutorService MODEL_CALLS = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * @param timeout how long to wait for the model; then the review fails as timed out and a late answer
     *                is not saved (runbuddy.ai.review-timeout)
     */
    public RunReviewService(RunResultRepository runs, RunFileRepository files, ActivityFileReader reader,
            RunReviewRepository reviews, List<RunReviewer> reviewers, Clock clock,
            @Value("${runbuddy.ai.review-timeout:60s}") Duration timeout) {
        this.runs = runs;
        this.files = files;
        this.reader = reader;
        this.reviews = reviews;
        this.reviewers = List.copyOf(reviewers);
        this.clock = clock;
        this.timeout = timeout;
    }

    /**
     * @throws NoSuchElementException when there is no such run
     */
    public Optional<RunReview> saved(String activityId) {
        requireRun(activityId);
        return reviews.find(activityId);
    }

    /**
     * Reviews the run now, replacing any saved review.
     *
     * @throws NoSuchElementException when there is no such run
     * @throws AdvisorUnavailableException when no AI is configured or it fails; nothing is saved then
     */
    public RunReview review(String activityId) {
        RunResult run = requireRun(activityId);
        RunReviewer reviewer = chooseReviewer();
        RunReviewRequest request = new RunReviewRequest(run, storedLaps(activityId), SimilarRuns.forRun(run, recentRuns(run)));

        RunReviewer.Written written = withinTimeout(reviewer, request);

        RunReview review = new RunReview(activityId, written.headline(), written.runItself(), written.comparison(),
                reviewer.id(), reviewer.model(), Instant.now(clock));
        reviews.save(review);
        log.info("Run {} reviewed by {} ({}), compared with {} similar runs, {} laps",
                activityId, reviewer.id(), reviewer.model(), request.similar().runs().size(), request.laps().size());
        return review;
    }

    /** Waits for the model at most {@code timeout}; the answer is saved only by the caller, so a late one is dropped. */
    private RunReviewer.Written withinTimeout(RunReviewer reviewer, RunReviewRequest request) {
        Future<RunReviewer.Written> call = MODEL_CALLS.submit(() -> reviewer.review(request));
        try {
            return call.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            call.cancel(true);
            log.warn("{} ({}) did not review run {} within {} s", reviewer.id(), reviewer.model(),
                    request.run().activityId(), timeout.toSeconds());
            throw new AdvisorUnavailableException(AdvisorUnavailableException.Reason.TIMED_OUT,
                    reviewer.id() + " did not answer within " + timeout.toSeconds() + " s", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            throw new AdvisorUnavailableException(reviewer.id() + " could not review the run: " + e.getCause(), e.getCause());
        } catch (InterruptedException e) {
            call.cancel(true);
            Thread.currentThread().interrupt();
            throw new AdvisorUnavailableException(reviewer.id() + " review was interrupted", e);
        }
    }

    private RunResult requireRun(String activityId) {
        return runs.findById(activityId).orElseThrow(() -> new NoSuchElementException("no run " + activityId));
    }

    /** A real model when one is configured; the rule-based fake otherwise. */
    private RunReviewer chooseReviewer() {
        return reviewers.stream().filter(r -> !r.id().equals("fake")).findFirst()
                .or(() -> reviewers.stream().findFirst())
                .orElseThrow(() -> new AdvisorUnavailableException("no AI is configured to review runs", null));
    }

    private List<Lap> storedLaps(String activityId) {
        try {
            return files.find(activityId).map(zip -> reader.read(zip).laps()).orElse(List.of());
        } catch (UnreadableActivityFileException e) {
            log.warn("Run {}: activity file unreadable, reviewing without laps ({})", activityId, e.getMessage());
            return List.of();
        }
    }

    /** This month and the one before, which covers the look-back of {@link SimilarRuns}. */
    private List<RunResult> recentRuns(RunResult run) {
        YearMonth month = YearMonth.from(run.date());
        List<RunResult> recent = new ArrayList<>(runs.findByMonth(month.minusMonths(1)));
        recent.addAll(runs.findByMonth(month));
        return recent;
    }
}
