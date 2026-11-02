package com.example.runbuddy.activity.fit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.example.runbuddy.activity.Lap;
import com.example.runbuddy.activity.RunDetail;
import com.example.runbuddy.activity.TrackPoint;
import com.example.runbuddy.activity.UnreadableActivityFileException;
import com.example.runbuddy.shared.Distance;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class FitRunDetailReaderTest {

    private final FitRunDetailReader reader = new FitRunDetailReader();

    @Test
    void readsEachLapWithCadenceInStepsPerMinute() {
        byte[] fit = new SyntheticFit()
                .lap(0, 1000f, 330f, 150, 162, 85, 0.5f, 92, 8)
                .lap(330, 1000f, 320f, 158, 171, 87, null, 95, 3)
                .zip();

        RunDetail detail = reader.read(fit);

        assertThat(detail.laps()).containsExactly(
                // Per-leg 85.5 is 171 steps a minute; the maximum has no fraction: 92 * 2.
                new Lap(1, Duration.ZERO, Distance.ofMeters(1000), Duration.ofSeconds(330), 150, 162, 171, 184, 8),
                new Lap(2, Duration.ofSeconds(330), Distance.ofMeters(1000), Duration.ofSeconds(320), 158, 171, 174, 190, 3));
        assertThat(detail.laps().getFirst().pace()).contains(Duration.ofSeconds(330));
    }

    @Test
    void readsTheTrackFromTheRecordsThatHaveAPosition() {
        byte[] fit = new SyntheticFit()
                .record(0, 0.0001, 0.0002, 120)
                .record(7, null, null, 125)
                .record(14, 0.0003, -0.0004, null)
                .lap(0, 50f, 14f, null, null, null, null, null, null)
                .zip();

        RunDetail detail = reader.read(fit);

        assertThat(detail.track()).hasSize(2);
        TrackPoint first = detail.track().getFirst();
        assertThat(first.offset()).isEqualTo(Duration.ZERO);
        assertThat(first.latitude()).isCloseTo(0.0001, within(1e-6));
        assertThat(first.longitude()).isCloseTo(0.0002, within(1e-6));
        assertThat(first.heartRate()).isEqualTo(120);
        assertThat(first.altitudeMeters()).isCloseTo(10.4, within(0.01));
        TrackPoint last = detail.track().getLast();
        assertThat(last.offset()).isEqualTo(Duration.ofSeconds(14));
        assertThat(last.longitude()).isCloseTo(-0.0004, within(1e-6));
        assertThat(last.heartRate()).isNull();
    }

    @Test
    void aValueTheDomainWouldRejectIsDroppedNotTheLap() {
        // A max below its average (a watch glitch) and a 0 from a missing sensor.
        byte[] fit = new SyntheticFit().lap(0, 1000f, 330f, 150, 140, 0, null, 0, null).zip();

        assertThat(reader.read(fit).laps()).singleElement().satisfies(lap -> {
            assertThat(lap.averageHeartRate()).isEqualTo(150);
            assertThat(lap.maxHeartRate()).isNull();
            assertThat(lap.averageCadence()).isNull();
            assertThat(lap.maxCadence()).isNull();
        });
    }

    @Test
    void aFileWithoutLapsOrPositionsGivesAnEmptyDetail() {
        RunDetail detail = reader.read(new SyntheticFit().zip());

        assertThat(detail.laps()).isEmpty();
        assertThat(detail.track()).isEmpty();
    }

    @Test
    void somethingThatIsNotAZipIsUnreadable() {
        assertThatThrownBy(() -> reader.read("not a zip".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(UnreadableActivityFileException.class);
    }

    @Test
    void aZipWithoutAFitFileIsUnreadable() {
        assertThatThrownBy(() -> reader.read(SyntheticFit.zipped("readme.txt", "hello".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(UnreadableActivityFileException.class);
    }

    @Test
    void aBrokenFitFileIsUnreadable() {
        assertThatThrownBy(() -> reader.read(SyntheticFit.zipped("x_ACTIVITY.fit", new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14})))
                .isInstanceOf(UnreadableActivityFileException.class);
    }
}
