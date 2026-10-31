package com.example.runbuddy.activity;

/** The {@link ActivitySource} could not deliver runs. Not the user's fault, but some reasons need them to act. */
public class ActivitySourceUnavailableException extends RuntimeException {

    public enum Reason {
        /** The source refused our credentials (for Garmin: the API key expired, was deleted or is wrong). */
        CREDENTIALS_REJECTED,
        /** The source's rate limit was hit; trying again later may work. */
        RATE_LIMITED,
        /** Network failure, server error or a response we could not read. */
        UNAVAILABLE
    }

    private final Reason reason;

    public ActivitySourceUnavailableException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
