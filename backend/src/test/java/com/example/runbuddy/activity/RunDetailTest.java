package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.shared.Distance;
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

    @Test
    void thinningKeepsTheFirstPointOfEveryLapSoAShortLapStillHasALine() {
        // 1,000 points 7 s apart; a 21-second lap (3 points) that even sampling at 50 points would skip.
        List<Lap> laps = List.of(lap(1, 0), lap(2, 3500), lap(3, 3521));
        RunDetail detail = new RunDetail(laps, track(1000));

        List<TrackPoint> route = detail.route(50);

        assertThat(route).extracting(TrackPoint::offset)
                .contains(Duration.ofSeconds(3500), Duration.ofSeconds(3521))
                .isSorted()
                .doesNotHaveDuplicates();
    }

    @Test
    void aPointBelongsToTheLapThatHadStartedByThen() {
        RunDetail detail = new RunDetail(List.of(lap(1, 0), lap(2, 330), lap(3, 660)), List.of());

        assertThat(detail.lapAt(Duration.ZERO)).hasValue(1);
        assertThat(detail.lapAt(Duration.ofSeconds(329))).hasValue(1);
        assertThat(detail.lapAt(Duration.ofSeconds(330))).hasValue(2);
        assertThat(detail.lapAt(Duration.ofSeconds(5000))).hasValue(3);
    }

    @Test
    void aPointBeforeTheFirstLapOrInARunWithoutLapsBelongsToNoLap() {
        assertThat(new RunDetail(List.of(lap(1, 10)), List.of()).lapAt(Duration.ofSeconds(3))).isEmpty();
        assertThat(new RunDetail(List.of(), List.of()).lapAt(Duration.ZERO)).isEmpty();
    }

    private static Lap lap(int number, long startSeconds) {
        return new Lap(number, Duration.ofSeconds(startSeconds), Distance.ofKilometers(1), Duration.ofSeconds(300),
                null, null, null, null, null);
    }

    private static List<TrackPoint> track(int points) {
        return IntStream.range(0, points)
                .mapToObj(i -> new TrackPoint(Duration.ofSeconds(i * 7L), 0.0001 * i, 0.0001, null, null))
                .toList();
    }
}
