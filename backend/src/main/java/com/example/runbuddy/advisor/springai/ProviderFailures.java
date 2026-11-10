package com.example.runbuddy.advisor.springai;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Says why a provider call failed. Spring AI wraps the provider's error ("Failed to generate content"),
 * and the reason the runner can act on (e.g. "This model ... is no longer available") is in the cause.
 */
final class ProviderFailures {

    private static final int MAX_DEPTH = 10;

    private ProviderFailures() {
    }

    /** The message of the deepest cause that has one, e.g. the provider's own error text. */
    static String reason(Throwable failure) {
        return Stream.iterate(failure, Objects::nonNull, Throwable::getCause).limit(MAX_DEPTH)
                .map(Throwable::getMessage)
                .filter(message -> message != null && !message.isBlank())
                .reduce((outer, inner) -> inner)
                .orElse(failure.getClass().getSimpleName());
    }
}
