package com.example.runbuddy.activity.garmin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Exchanges the Personal API key for an access token and keeps it until shortly before it expires.
 *
 * <p>Garmin issues tokens for 24 hours (or until the key expires, if sooner) and has no refresh
 * tokens: a new one is simply requested with the key. We reuse a token for {@link #LIFETIME} and
 * callers {@link #invalidate()} it when Garmin answers 401, e.g. after the key was replaced.
 */
final class GarminTokens {

    static final String TOKEN_PATH = "/personalapitoken-service/personal-api/personal-api-key/token";

    /** An hour short of Garmin's 24 hours, so a token never expires in the middle of an import. */
    static final Duration LIFETIME = Duration.ofHours(23);

    private final RestClient client;
    private final String apiKey;
    private final Clock clock;

    private String token;
    private Instant expiresAt = Instant.MIN;

    GarminTokens(RestClient client, String apiKey, Clock clock) {
        this.client = client;
        this.apiKey = apiKey;
        this.clock = clock;
    }

    /** A valid token, requesting a new one when needed. Throws the client's exceptions as they are. */
    synchronized String current() {
        if (token == null || !clock.instant().isBefore(expiresAt)) {
            token = obtain();
            expiresAt = clock.instant().plus(LIFETIME);
        }
        return token;
    }

    synchronized void invalidate() {
        token = null;
    }

    /** The response is the bare token (a JWT) as text/plain, not JSON. */
    private String obtain() {
        String body = client.post()
                .uri(TOKEN_PATH)
                .header(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey)
                .retrieve()
                .body(String.class);
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Garmin returned an empty access token");
        }
        return body.strip();
    }
}
