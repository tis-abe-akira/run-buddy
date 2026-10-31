package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RunMetricsTest {

    @Test
    void everyMeasurementMayBeMissing() {
        assertThat(RunMetrics.NONE).hasAllNullFieldsOrProperties();
    }

    @Test
    void noClimbIsAMeasurementToo() {
        assertThat(new RunMetrics(null, null, null, null, 0).elevationGainMeters()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void sensorValuesMustBePositive(int value) {
        assertThatThrownBy(() -> new RunMetrics(value, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RunMetrics(null, value, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RunMetrics(null, null, value, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RunMetrics(null, null, null, value, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeClimbIsRejected() {
        assertThatThrownBy(() -> new RunMetrics(null, null, null, null, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void maximumCadenceBelowTheAverageIsRejected() {
        assertThatThrownBy(() -> new RunMetrics(null, 172, 171, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxCadence");
    }
}
