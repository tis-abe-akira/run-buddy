package com.example.runbuddy.activity;

import java.time.Duration;
import java.util.Objects;

/**
 * Where the runner was at one moment of the run (ADR-0012). It locates the runner, so it never goes
 * to the AI advisor.
 *
 * @param offset        time from the start of the run
 * @param latitude      degrees, -90 to 90
 * @param longitude     degrees, -180 to 180
 * @param heartRate     beats per minute, or null
 * @param altitudeMeters meters above sea level, or null
 */
public record TrackPoint(Duration offset, double latitude, double longitude, Integer heartRate, Double altitudeMeters) {

    public TrackPoint {
        Objects.requireNonNull(offset, "offset");
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("not a place on Earth: " + latitude + ", " + longitude);
        }
        if (heartRate != null && heartRate <= 0) {
            throw new IllegalArgumentException("heartRate must be positive: " + heartRate);
        }
    }
}
