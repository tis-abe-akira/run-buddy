package com.example.runbuddy.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs/{month}")
public class RunController {

    private final RunImportService runImportService;
    private final RunResultRepository runResultRepository;

    public RunController(RunImportService runImportService, RunResultRepository runResultRepository) {
        this.runImportService = runImportService;
        this.runResultRepository = runResultRepository;
    }

    @GetMapping
    public List<RunResponse> list(@PathVariable YearMonth month) {
        return runResultRepository.findByMonth(month).stream().map(RunResponse::from).toList();
    }

    @PostMapping("/import")
    public RunImportService.ImportResult importRuns(@PathVariable YearMonth month) {
        return runImportService.importRuns(month);
    }

    /**
     * The problem's "reason" property is the {@link ActivitySourceUnavailableException.Reason}, so the
     * screen can tell the user what to do (renew the key, wait, check the network).
     */
    @ExceptionHandler(ActivitySourceUnavailableException.class)
    ProblemDetail handleUnavailable(ActivitySourceUnavailableException e) {
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

    public record RunResponse(
            String activityId,
            LocalDateTime startedAt,
            double distanceKm,
            long durationSeconds,
            long paceSecondsPerKm,
            @Schema(types = {"integer", "null"}) Integer averageHeartRate) {

        static RunResponse from(RunResult run) {
            return new RunResponse(
                    run.activityId(),
                    run.startedAt(),
                    run.distance().toKilometers(),
                    run.duration().toSeconds(),
                    run.pace().toSeconds(),
                    run.averageHeartRate());
        }
    }
}
