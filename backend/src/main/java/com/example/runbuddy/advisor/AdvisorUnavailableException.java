package com.example.runbuddy.advisor;

/** The provider could not give an answer (network, quota, refusal...). Not the user's fault. */
public class AdvisorUnavailableException extends RuntimeException {

    public AdvisorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
