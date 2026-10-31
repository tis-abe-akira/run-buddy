package com.example.runbuddy.advisor;

/** The provider could not give an answer (network, quota, refusal...). Not the user's fault. */
public class AdvisorUnavailableException extends RuntimeException {

    public enum Reason {
        /** The provider's rate limit or quota was hit; trying again later may work. */
        RATE_LIMITED,
        FAILED
    }

    private final Reason reason;

    public AdvisorUnavailableException(String message, Throwable cause) {
        this(Reason.FAILED, message, cause);
    }

    public AdvisorUnavailableException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
