package com.example.runbuddy.review;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunsImported;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Gives each newly imported run a one-line review (#36), when switched on with
 * {@code runbuddy.ai.auto-review=true} (off by default: it sends every new run to the AI).
 *
 * <p>The import has already returned by then: the reviews are written one run after another on a
 * background thread, so a slow or failing AI never delays or fails an import. A run whose one-liner
 * fails simply has none; the detailed review can still be asked for.
 */
@Component
public class ImportedRunsReviewer {

    private static final Logger log = LoggerFactory.getLogger(ImportedRunsReviewer.class);

    private final RunReviewService service;
    private final boolean enabled;
    private final Executor background;

    @Autowired
    public ImportedRunsReviewer(RunReviewService service, @Value("${runbuddy.ai.auto-review:false}") boolean enabled) {
        this(service, enabled, Executors.newVirtualThreadPerTaskExecutor());
    }

    /** @param background where the reviews are written; tests pass one that runs them at once */
    ImportedRunsReviewer(RunReviewService service, boolean enabled, Executor background) {
        this.service = service;
        this.enabled = enabled;
        this.background = background;
    }

    @EventListener
    public void onImported(RunsImported imported) {
        if (!enabled) {
            return;
        }
        background.execute(() -> imported.added().forEach(this::reviewQuietly));
    }

    private void reviewQuietly(RunResult run) {
        try {
            service.reviewBriefly(run.activityId());
        } catch (RuntimeException e) {
            log.warn("Run {}: no one-liner, the AI could not write it ({})", run.activityId(), e.getMessage());
        }
    }
}
