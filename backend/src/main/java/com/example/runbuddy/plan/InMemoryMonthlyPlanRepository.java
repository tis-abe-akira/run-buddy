package com.example.runbuddy.plan;

import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** Phase 1 storage. Data is lost on restart. */
@Repository
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
