package com.example.runbuddy.advisor.springai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProviderFailuresTest {

    /** Spring AI's "Failed to generate content" hides why; the provider's message says what to do. */
    @Test
    void givesTheProvidersOwnReason() {
        var provider = new IllegalStateException("404 . This model models/old-flash is no longer available to new users.");
        var wrapped = new RuntimeException("Failed to generate content", provider);

        assertThat(ProviderFailures.reason(wrapped)).isEqualTo("404 . This model models/old-flash is no longer available to new users.");
    }

    @Test
    void skipsCausesWithoutAMessage() {
        var wrapped = new RuntimeException("Failed to generate content", new IllegalStateException());

        assertThat(ProviderFailures.reason(wrapped)).isEqualTo("Failed to generate content");
        assertThat(ProviderFailures.reason(new IllegalStateException())).isEqualTo("IllegalStateException");
    }
}
