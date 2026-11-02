package com.example.runbuddy.activity;

import java.util.List;

/** What a run's activity file adds to its summary: the laps and the track (ADR-0012). */
public record RunDetail(List<Lap> laps, List<TrackPoint> track) {

    public RunDetail {
        laps = List.copyOf(laps);
        track = List.copyOf(track);
    }
}
