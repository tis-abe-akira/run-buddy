package com.example.runbuddy.activity;

import java.util.Optional;

/**
 * Port for the original activity files of stored runs, zipped as Garmin sends them (ADR-0012).
 * Implementations: in memory, jOOQ (persistence/jooq).
 */
public interface RunFileRepository {

    /** Keeps the file of a stored run. If the run already has one, the first is kept. */
    void save(String activityId, byte[] fitZip);

    Optional<byte[]> find(String activityId);
}
