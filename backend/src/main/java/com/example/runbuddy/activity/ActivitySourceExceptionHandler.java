package com.example.runbuddy.activity;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Every endpoint that asks the activity source reports its failures the same way (ADR-0009, ADR-0012). */
@RestControllerAdvice
public class ActivitySourceExceptionHandler {

    /**
     * The problem's "reason" property is the {@link ActivitySourceUnavailableException.Reason}, so the
     * screen can tell the user what to do (renew the key, wait, check the network).
     */
    @ExceptionHandler(ActivitySourceUnavailableException.class)
    public ProblemDetail handleUnavailable(ActivitySourceUnavailableException e) {
        HttpStatus status = e.reason() == ActivitySourceUnavailableException.Reason.RATE_LIMITED
                ? HttpStatus.TOO_MANY_REQUESTS
                : HttpStatus.BAD_GATEWAY;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setTitle(switch (e.reason()) {
            case CREDENTIALS_REJECTED -> "The activity source rejected the credentials";
            case RATE_LIMITED -> "Rate limited";
            case UNAVAILABLE -> "The activity source is unavailable";
        });
        problem.setProperty("reason", e.reason().name());
        return problem;
    }

    /** A run's activity file that cannot be read; "reason" lets the screen say so (ADR-0012). */
    @ExceptionHandler(UnreadableActivityFileException.class)
    public ProblemDetail handleUnreadable(UnreadableActivityFileException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        problem.setTitle("The activity file cannot be read");
        problem.setProperty("reason", "UNREADABLE_FILE");
        return problem;
    }
}
