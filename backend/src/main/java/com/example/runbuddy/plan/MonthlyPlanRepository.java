package com.example.runbuddy.plan;

import java.time.YearMonth;
import java.util.Optional;

public interface MonthlyPlanRepository {

    void save(MonthlyPlan plan);

    Optional<MonthlyPlan> findByMonth(YearMonth month);

    /** The stored plan, or an empty one when the month has not been planned. */
    default MonthlyPlan getOrEmpty(YearMonth month) {
        return findByMonth(month).orElseGet(() -> MonthlyPlan.empty(month));
    }
}
