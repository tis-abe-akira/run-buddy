package com.example.runbuddy.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Guards the database setup itself, independent of the persistence library: what happens when
 * code forgets the transaction that carries the current user.
 */
class DatabaseSafetyTest extends DatabaseTest {

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MonthlyPlanRepository plans;

    @Autowired
    TransactionTemplate transactions;

    @Test
    void writingOutsideATransactionFailsLoudly() {
        assertThatThrownBy(() -> jdbc.sql("insert into monthly_plan (month, target_meters) values ('2026-11', 0)").update())
                .rootCause()
                .hasMessageContaining("app.user_id is not set");
    }

    @Test
    void readingOutsideATransactionFailsLoudlyInsteadOfSeeingNothing() {
        plans.save(MonthlyPlan.empty(YearMonth.of(2026, 11)));

        assertThatThrownBy(() -> jdbc.sql("select count(*) from monthly_plan").query(Long.class).single())
                .rootCause()
                .hasMessageContaining("app.user_id is not set");
    }

    @Test
    void readingInsideATransactionSeesOnlyTheCurrentUsersRows() {
        plans.save(MonthlyPlan.empty(YearMonth.of(2026, 11)));

        Long count = transactions.execute(status -> jdbc.sql("select count(*) from monthly_plan").query(Long.class).single());

        assertThat(count).isEqualTo(1);
    }
}
