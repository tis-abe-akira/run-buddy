package com.example.runbuddy.export;

import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExportController {

    static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final MonthlyPlanRepository plans;
    private final RunResultRepository runs;

    public ExportController(MonthlyPlanRepository plans, RunResultRepository runs) {
        this.plans = plans;
        this.runs = runs;
    }

    /** The month's plan and runs, one line per day, as a CSV download (run-buddy-2026-11.csv). */
    @GetMapping("/api/export/{month}.csv")
    public ResponseEntity<String> exportMonth(@PathVariable YearMonth month) {
        MonthlyPlan plan = plans.findByMonth(month).orElseGet(() -> MonthlyPlan.empty(month));
        String csv = DailyLogCsv.write(DailyLog.of(plan, runs.findByMonth(month)));
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("run-buddy-" + month + ".csv")
                        .build()
                        .toString())
                .body(csv);
    }
}
