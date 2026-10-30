package com.example.runbuddy.health;

/**
 * Response body for {@code GET /health}.
 */
public record HealthResponse(String status) {

    public static HealthResponse ok() {
        return new HealthResponse("ok");
    }
}
