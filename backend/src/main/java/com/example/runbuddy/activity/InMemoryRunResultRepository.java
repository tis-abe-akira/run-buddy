package com.example.runbuddy.activity;

import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** Phase 1 storage. Data is lost on restart. */
@Repository
public class InMemoryRunResultRepository implements RunResultRepository {

    private final Map<String, RunResult> runsById = new ConcurrentHashMap<>();

    @Override
    public int saveAll(Collection<RunResult> runs) {
        int added = 0;
        for (RunResult run : runs) {
            if (runsById.put(run.activityId(), run) == null) {
                added++;
            }
        }
        return added;
    }

    @Override
    public List<RunResult> findByMonth(YearMonth month) {
        return runsById.values().stream()
                .filter(run -> YearMonth.from(run.startedAt()).equals(month))
                .sorted(Comparator.comparing(RunResult::startedAt))
                .toList();
    }
}
