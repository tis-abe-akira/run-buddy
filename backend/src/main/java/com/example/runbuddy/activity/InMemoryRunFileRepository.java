package com.example.runbuddy.activity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory storage for the "memory" profile and for tests. Data is lost on restart. */
public class InMemoryRunFileRepository implements RunFileRepository {

    private final Map<String, byte[]> filesByRun = new ConcurrentHashMap<>();

    @Override
    public void save(String activityId, byte[] fitZip) {
        filesByRun.putIfAbsent(activityId, fitZip.clone());
    }

    @Override
    public Optional<byte[]> find(String activityId) {
        return Optional.ofNullable(filesByRun.get(activityId)).map(byte[]::clone);
    }
}
