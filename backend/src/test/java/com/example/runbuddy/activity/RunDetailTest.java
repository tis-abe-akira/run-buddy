package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RunDetailTest {

    @Test
    void aShortTrackIsDrawnAsItIs() {
        RunDetail detail = new RunDetail(List.of(), track(3));

        assertThat(detail.route(300)).containsExactlyElementsOf(detail.track());
    }

    @Test
    void aLongTrackIsThinnedToAtMostTheLimitKeepingTheStartAndTheFinish() {
        RunDetail detail = new RunDetail(List.of(), track(1800));

        List<TrackPoint> route = detail.route(300);

        assertThat(route).hasSize(300);
        assertThat(route.getFirst()).isEqualTo(detail.track().getFirst());
        assertThat(route.getLast()).isEqualTo(detail.track().getLast());
    }

    @Test
    void thinningKeepsThePointsInTheOrderTheRunnerPassedThem() {
        List<TrackPoint> route = new RunDetail(List.of(), track(1000)).route(50);

        assertThat(route).extracting(TrackPoint::offset).isSorted();
    }

    @Test
    void aRunWithoutPositionsHasNoRoute() {
        assertThat(new RunDetail(List.of(), List.of()).route(300)).isEmpty();
    }

    @Test
    void aRouteNeedsAtLeastTwoPoints() {
        assertThatThrownBy(() -> new RunDetail(List.of(), track(10)).route(1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<TrackPoint> track(int points) {
        return IntStream.range(0, points)
                .mapToObj(i -> new TrackPoint(Duration.ofSeconds(i * 7L), 0.0001 * i, 0.0001, null, null))
                .toList();
    }
}
