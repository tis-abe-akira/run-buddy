package com.example.runbuddy.persistence.jdbc;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JdbcMonthlyPlanRepository implements MonthlyPlanRepository {

    private final JdbcAggregateTemplate template;

    public JdbcMonthlyPlanRepository(JdbcAggregateTemplate template) {
        this.template = template;
    }

    @Override
    public void save(MonthlyPlan plan) {
        MonthlyPlanRow row = MonthlyPlanRow.from(plan);
        // The id is assigned (the month), so Spring Data cannot tell a new plan from an existing
        // one by itself. Updating an aggregate deletes and re-inserts its workouts.
        if (template.existsById(row.month(), MonthlyPlanRow.class)) {
            template.update(row);
        } else {
            template.insert(row);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MonthlyPlan> findByMonth(YearMonth month) {
        return Optional.ofNullable(template.findById(month.toString(), MonthlyPlanRow.class))
                .map(MonthlyPlanRow::toPlan);
    }
}
