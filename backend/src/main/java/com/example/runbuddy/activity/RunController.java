package com.example.runbuddy.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
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

    /** Re-reads the whole month from the source (the "latest" import is in {@link LatestImportController}). */
    @PostMapping("/import")
    public RunImportService.ImportResult importRuns(@PathVariable YearMonth month) {
        return runImportService.importRuns(month);
    }

    /** A run with every measurement we have; the optional ones are null when not recorded. */
    public record RunResponse(
            String activityId,
            LocalDateTime startedAt,
            double distanceKm,
            long durationSeconds,
            long paceSecondsPerKm,
            @Schema(types = {"integer", "null"}) Integer averageHeartRate,
            @Schema(types = {"integer", "null"}, description = "beats per minute") Integer maxHeartRate,
            @Schema(types = {"integer", "null"}, description = "steps per minute") Integer averageCadence,
            @Schema(types = {"integer", "null"}, description = "steps per minute") Integer maxCadence,
            @Schema(types = {"integer", "null"}, description = "centimeters") Integer averageStrideLengthCm,
            @Schema(types = {"integer", "null"}, description = "meters") Integer elevationGainMeters) {

        public static RunResponse from(RunResult run) {
            RunMetrics metrics = run.metrics();
            return new RunResponse(
                    run.activityId(),
                    run.startedAt(),
                    run.distance().toKilometers(),
                    run.duration().toSeconds(),
                    run.pace().toSeconds(),
                    run.averageHeartRate(),
                    metrics.maxHeartRate(),
                    metrics.averageCadence(),
                    metrics.maxCadence(),
                    metrics.averageStrideLengthCm(),
                    metrics.elevationGainMeters());
        }
    }
}
