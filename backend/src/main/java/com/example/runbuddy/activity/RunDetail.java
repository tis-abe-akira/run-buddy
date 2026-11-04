package com.example.runbuddy.activity;

import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/** What a run's activity file adds to its summary: the laps and the track (ADR-0012). */
public record RunDetail(List<Lap> laps, List<TrackPoint> track) {

    public RunDetail {
        laps = List.copyOf(laps);
        track = List.copyOf(track);
    }

    /**
     * The track thinned for drawing: about {@code maxPoints} points, evenly spread, always including the
     * start, the finish and the first point of every lap, so that a short lap (a 200 m recovery) still
     * has a line to highlight. A run has about 1,800 points, far more than a map line needs.
     */
    public List<TrackPoint> route(int maxPoints) {
        if (maxPoints < 2) {
            throw new IllegalArgumentException("a route needs at least 2 points: " + maxPoints);
        }
        if (track.size() <= maxPoints) {
            return track;
        }
        int last = track.size() - 1;
        IntStream evenlySpread = IntStream.range(0, maxPoints).map(i -> (int) Math.round((double) i * last / (maxPoints - 1)));
        IntStream lapStarts = laps.stream().mapToInt(lap -> firstPointFrom(lap.startOffset())).filter(i -> i >= 0);
        return IntStream.concat(evenlySpread, lapStarts).sorted().distinct().mapToObj(track::get).toList();
    }

    /** The number of the lap the runner was on at {@code offset}: the last lap that had started by then. */
    public OptionalInt lapAt(Duration offset) {
        return laps.stream()
                .filter(lap -> lap.startOffset().compareTo(offset) <= 0)
                .mapToInt(Lap::number)
                .reduce((earlier, later) -> later);
    }

    private int firstPointFrom(Duration offset) {
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).offset().compareTo(offset) >= 0) {
                return i;
            }
        }
        return -1;
    }
}
