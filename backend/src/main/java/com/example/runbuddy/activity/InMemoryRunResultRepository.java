package com.example.runbuddy.activity;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory storage for the "memory" profile and for tests. Data is lost on restart. */
public class InMemoryRunResultRepository implements RunResultRepository {

    private final Map<String, RunResult> runsById = new ConcurrentHashMap<>();

    @Override
    public List<RunResult> saveAll(Collection<RunResult> runs) {
        Map<String, RunResult> added = new LinkedHashMap<>();
        for (RunResult run : runs) {
            boolean stored = runsById.containsKey(run.activityId()) && !added.containsKey(run.activityId());
            runsById.put(run.activityId(), run);
            if (!stored) {
                added.put(run.activityId(), run);
            }
        }
        return List.copyOf(added.values());
    }

    @Override
    public List<RunResult> findByMonth(YearMonth month) {
        return runsById.values().stream()
                .filter(run -> YearMonth.from(run.startedAt()).equals(month))
                .sorted(Comparator.comparing(RunResult::startedAt))
                .toList();
    }

    @Override
    public Optional<RunResult> findById(String activityId) {
        return Optional.ofNullable(runsById.get(activityId));
    }

    @Override
    public Optional<LocalDateTime> latestStart() {
        return runsById.values().stream().map(RunResult::startedAt).max(Comparator.naturalOrder());
    }
}
