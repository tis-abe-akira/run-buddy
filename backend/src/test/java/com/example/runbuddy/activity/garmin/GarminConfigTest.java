package com.example.runbuddy.activity.garmin;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.ActivitySource;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Garmin is used exactly when chosen, and choosing it without a key stops startup. No request is sent. */
class GarminConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GarminConfig.class)
            .withBean(Clock.class, Clock::systemUTC)
            .withPropertyValues("runbuddy.garmin.base-url=https://garmin.test");

    @Test
    void usedWhenChosenWithAKey() {
        runner.withPropertyValues("runbuddy.activity-source=garmin", "runbuddy.garmin.api-key=some-key")
                .run(context -> assertThat(context).getBean(ActivitySource.class).isInstanceOf(GarminActivitySource.class));
    }

    @Test
    void chosenWithoutAKeyFailsToStartWithAnExplanation() {
        runner.withPropertyValues("runbuddy.activity-source=garmin", "runbuddy.garmin.api-key= ")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("GARMIN_API_KEY"));
    }

    @Test
    void notUsedOtherwise() {
        runner.withPropertyValues("runbuddy.garmin.api-key=some-key")
                .run(context -> assertThat(context).doesNotHaveBean(ActivitySource.class));
        runner.withPropertyValues("runbuddy.activity-source=dummy")
                .run(context -> assertThat(context).doesNotHaveBean(ActivitySource.class));
    }
}
