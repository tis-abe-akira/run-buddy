package com.example.runbuddy.activity;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Every import endpoint reports a failing activity source the same way (ADR-0009). */
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
}
