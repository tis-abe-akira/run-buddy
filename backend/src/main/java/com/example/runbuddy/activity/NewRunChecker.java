package com.example.runbuddy.activity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Whether the activity source has runs not imported yet (#40), asked for by the screen every
 * {@code runbuddy.activity.check-interval} (3 minutes by default).
 *
 * <p>Garmin's rate limit is unknown (ADR-0009), so the source is asked at most once per interval however
 * many screens are open: the answer, or the failure, is kept for the interval and shared. Callers wait for
 * a check in progress rather than start another. An import forgets the answer, so a run just imported is
 * not offered again.
 */
@Service
public class NewRunChecker {

    private final RunImportService imports;
    private final Clock clock;
    private final Duration interval;

    private Checked last;
    private Instant lastAt;
    private ActivitySourceUnavailableException lastFailure;

    public NewRunChecker(RunImportService imports, Clock clock,
            @Value("${runbuddy.activity.check-interval:3m}") Duration interval) {
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("runbuddy.activity.check-interval must be positive: " + interval);
        }
        this.imports = imports;
        this.clock = clock;
        this.interval = interval;
    }

    /** How often the screen should ask; also how long an answer is kept. */
    public Duration interval() {
        return interval;
    }

    /**
     * @throws ActivitySourceUnavailableException when the source failed, now or within the interval
     */
    public synchronized Checked check() {
        Instant now = clock.instant();
        if (lastAt != null && now.isBefore(lastAt.plus(interval))) {
            if (lastFailure != null) {
                throw lastFailure;
            }
            return last;
        }
        lastAt = now;
        try {
            last = new Checked(imports.findNewRuns(), now);
            lastFailure = null;
            return last;
        } catch (ActivitySourceUnavailableException e) {
            last = null;
            lastFailure = e;
            throw e;
        }
    }

    /** Runs were imported: the next check asks the source again. Waits for a check in progress. */
    @EventListener
    public synchronized void onImported(RunsImported imported) {
        lastAt = null;
        last = null;
        lastFailure = null;
    }

    /**
     * @param newRuns   runs in the source that are not stored, newest first
     * @param checkedAt when the source was asked
     */
    public record Checked(List<NewRun> newRuns, Instant checkedAt) {

        public Checked {
            newRuns = List.copyOf(newRuns);
        }
    }
}
