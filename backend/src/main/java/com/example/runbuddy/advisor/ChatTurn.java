package com.example.runbuddy.advisor;

import java.util.Objects;

/** One message of a consultation. */
public record ChatTurn(Speaker speaker, String text) {

    public enum Speaker {
        USER,
        ADVISOR
    }

    public ChatTurn {
        Objects.requireNonNull(speaker, "speaker");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("a message needs some text");
        }
    }

    public static ChatTurn user(String text) {
        return new ChatTurn(Speaker.USER, text);
    }

    public static ChatTurn advisor(String text) {
        return new ChatTurn(Speaker.ADVISOR, text);
    }
}
