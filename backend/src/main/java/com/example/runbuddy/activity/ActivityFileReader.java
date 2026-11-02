package com.example.runbuddy.activity;

/** Reads a run's activity file into laps and a track (ADR-0012). The FIT implementation lives in activity/fit. */
public interface ActivityFileReader {

    /** @throws UnreadableActivityFileException when the file cannot be read */
    RunDetail read(byte[] zippedFile);
}
