package com.example.runbuddy.web;

import com.example.runbuddy.advisor.AdvisorUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns broken domain invariants into 400 responses, and an unavailable AI (the advisor, run reviews)
 * into 502, or 429 for a rate limit and 504 when it took too long (RFC 9457 problem details).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidInput(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    @ExceptionHandler(AdvisorUnavailableException.class)
    public ProblemDetail handleUnavailable(AdvisorUnavailableException e) {
        if (e.reason() == AdvisorUnavailableException.Reason.RATE_LIMITED) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                    e.getMessage() + ". Wait a minute and send it again, or choose another model.");
            problem.setTitle("Rate limited");
            return problem;
        }
        if (e.reason() == AdvisorUnavailableException.Reason.TIMED_OUT) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, e.getMessage());
            problem.setTitle("The AI took too long");
            return problem;
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        problem.setTitle("The advisor could not answer");
        return problem;
    }
}
