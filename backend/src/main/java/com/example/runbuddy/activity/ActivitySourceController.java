package com.example.runbuddy.activity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Which ActivitySource imports read from, so the screen can say when its runs are generated. */
@RestController
public class ActivitySourceController {

    private final String sourceId;

    public ActivitySourceController(@Value("${runbuddy.activity-source:dummy}") String sourceId) {
        this.sourceId = sourceId;
    }

    @GetMapping("/api/activity-source")
    public ActivitySourceInfo info() {
        return new ActivitySourceInfo(sourceId);
    }

    /** @param id "dummy" or "garmin" (runbuddy.activity-source) */
    public record ActivitySourceInfo(String id) {
    }
}
