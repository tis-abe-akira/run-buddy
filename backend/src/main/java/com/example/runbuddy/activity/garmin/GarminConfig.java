package com.example.runbuddy.activity.garmin;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Garmin as the {@link com.example.runbuddy.activity.ActivitySource}, with
 * {@code runbuddy.activity-source=garmin}.
 *
 * <p>Without an API key the application refuses to start instead of falling back to the dummy:
 * choosing garmin and silently getting generated runs would put made-up data into the database
 * next to real runs, with nothing on screen to tell them apart (ADR-0009).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "runbuddy.activity-source", havingValue = "garmin")
public class GarminConfig {

    @Bean
    GarminActivitySource garminActivitySource(
            @Value("${runbuddy.garmin.api-key:}") String apiKey,
            @Value("${runbuddy.garmin.base-url}") String baseUrl,
            Clock clock) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("runbuddy.activity-source=garmin needs a Garmin Personal API key. "
                    + "Set GARMIN_API_KEY (see .env.example), or set RUNBUDDY_ACTIVITY_SOURCE=dummy to use generated runs.");
        }
        return GarminActivitySource.connect(baseUrl, apiKey.strip(), clock);
    }
}
