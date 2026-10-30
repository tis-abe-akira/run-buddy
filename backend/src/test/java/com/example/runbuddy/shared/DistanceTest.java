package com.example.runbuddy.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DistanceTest {

    @Test
    void kilometersAreRoundedToTheNearestMeter() {
        assertThat(Distance.ofKilometers(10.0004).meters()).isEqualTo(10_000);
        assertThat(Distance.ofKilometers(10.0005).meters()).isEqualTo(10_001);
    }

    @Test
    void summingKeepsExactMeters() {
        // 0.1 km ten times would drift as doubles
        List<Distance> tenths = java.util.Collections.nCopies(10, Distance.ofKilometers(0.1));
        assertThat(Distance.sum(tenths)).isEqualTo(Distance.ofKilometers(1));
    }

    @Test
    void negativeDistanceIsRejected() {
        assertThatThrownBy(() -> Distance.ofKilometers(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonFiniteDistanceIsRejected() {
        assertThatThrownBy(() -> Distance.ofKilometers(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
