package com.example.runbuddy.plan;

import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory storage for the "memory" profile and for tests. Data is lost on restart. */
public class InMemoryMonthlyPlanRepository implements MonthlyPlanRepository {

    private final Map<YearMonth, MonthlyPlan> plansByMonth = new ConcurrentHashMap<>();

    @Override
    public void save(MonthlyPlan plan) {
        plansByMonth.put(plan.month(), plan);
    }

    @Override
    public Optional<MonthlyPlan> findByMonth(YearMonth month) {
        return Optional.ofNullable(plansByMonth.get(month));
    }
}
