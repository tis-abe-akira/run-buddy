package com.example.runbuddy.review;

import com.example.runbuddy.activity.ActivityFileReader;
import com.example.runbuddy.activity.Lap;
import com.example.runbuddy.activity.RunFileRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.activity.UnreadableActivityFileException;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public RunReviewService(RunResultRepository runs, RunFileRepository files, ActivityFileReader reader,
            RunReviewRepository reviews, List<RunReviewer> reviewers, Clock clock) {
        this.runs = runs;
        this.files = files;
        this.reader = reader;
        this.reviews = reviews;
        this.reviewers = List.copyOf(reviewers);
        this.clock = clock;
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

        RunReviewer.Written written = reviewer.review(request);

        RunReview review = new RunReview(activityId, written.headline(), written.runItself(), written.comparison(),
                reviewer.id(), reviewer.model(), Instant.now(clock));
        reviews.save(review);
        log.info("Run {} reviewed by {} ({}), compared with {} similar runs, {} laps",
                activityId, reviewer.id(), reviewer.model(), request.similar().runs().size(), request.laps().size());
        return review;
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
