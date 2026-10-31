package com.example.runbuddy.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Says at startup where runs will be imported from, so the log answers "real or dummy?" at once. */
@Component
class ActivitySourceStartupLog {

    private static final Logger log = LoggerFactory.getLogger(ActivitySourceStartupLog.class);

    private final String sourceId;
    private final ActivitySource source;

    ActivitySourceStartupLog(@Value("${runbuddy.activity-source:dummy}") String sourceId, ActivitySource source) {
        this.sourceId = sourceId;
        this.source = source;
    }

    @EventListener(ApplicationReadyEvent.class)
    void logSource() {
        if ("dummy".equals(sourceId)) {
            log.info("Activity source: dummy ({}) - imports generate runs, not real data. "
                    + "Set RUNBUDDY_ACTIVITY_SOURCE=garmin to import from Garmin.", source.getClass().getSimpleName());
        } else {
            log.info("Activity source: {} ({})", sourceId, source.getClass().getSimpleName());
        }
    }
}
