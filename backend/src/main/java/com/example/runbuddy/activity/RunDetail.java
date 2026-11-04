package com.example.runbuddy.activity;

import java.util.List;
import java.util.stream.IntStream;

/** What a run's activity file adds to its summary: the laps and the track (ADR-0012). */
public record RunDetail(List<Lap> laps, List<TrackPoint> track) {

    public RunDetail {
        laps = List.copyOf(laps);
        track = List.copyOf(track);
    }

    /**
     * The track thinned for drawing: at most {@code maxPoints} points, evenly spread, always including
     * the start and the finish. A run has about 1,800 points, far more than a map line needs.
     */
    public List<TrackPoint> route(int maxPoints) {
        if (maxPoints < 2) {
            throw new IllegalArgumentException("a route needs at least 2 points: " + maxPoints);
        }
        if (track.size() <= maxPoints) {
            return track;
        }
        int last = track.size() - 1;
        return IntStream.range(0, maxPoints)
                .mapToObj(i -> track.get((int) Math.round((double) i * last / (maxPoints - 1))))
                .toList();
    }
}
