package com.example.runbuddy.activity;

/** A stored or downloaded activity file cannot be read (not a zip, no FIT file in it, or a broken one). */
public class UnreadableActivityFileException extends RuntimeException {

    public UnreadableActivityFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
