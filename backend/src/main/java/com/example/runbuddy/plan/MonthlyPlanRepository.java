package com.example.runbuddy.plan;

import java.time.YearMonth;
import java.util.Optional;

public interface MonthlyPlanRepository {

    void save(MonthlyPlan plan);

    Optional<MonthlyPlan> findByMonth(YearMonth month);
}
