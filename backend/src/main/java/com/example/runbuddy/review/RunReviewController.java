package com.example.runbuddy.review;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A run's AI review (#35, ADR-0016): the saved one, or a new one on request. An AI failure is answered
 * like the advisor's (502, or 429 for a rate limit) by {@code ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/runs/{activityId}/review")
public class RunReviewController {

    private final RunReviewService service;

    public RunReviewController(RunReviewService service) {
        this.service = service;
    }

    /** The saved review; {@code review} is null when the run has not been reviewed yet. */
    @GetMapping
    public SavedReviewBody saved(@PathVariable String activityId) {
        return new SavedReviewBody(service.saved(activityId).map(ReviewBody::from).orElse(null));
    }

    /** Reviews the run now (it calls the AI), replacing any saved review. */
    @PostMapping
    public ReviewBody review(@PathVariable String activityId) {
        return ReviewBody.from(service.review(activityId));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail handleUnknownRun(NoSuchElementException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("No such run");
        return problem;
    }

    public record SavedReviewBody(@Schema(types = {"object", "null"}, implementation = ReviewBody.class) ReviewBody review) {
    }

    /**
     * @param brief      true for the one-liner written on import (#36): only the headline, until the detailed
     *                   review is asked for
     * @param runItself  the run on its own (viewpoint A); null when brief
     * @param comparison against similar recent runs (viewpoint C); null when brief
     */
    public record ReviewBody(
            String headline,
            boolean brief,
            @Schema(types = {"string", "null"}) String runItself,
            @Schema(types = {"string", "null"}) String comparison,
            String reviewer,
            String model,
            Instant reviewedAt) {

        static ReviewBody from(RunReview review) {
            return new ReviewBody(review.headline(), review.isBrief(), review.runItself(), review.comparison(),
                    review.reviewer(), review.model(), review.reviewedAt());
        }
    }
}
