package com.example.runbuddy.advisor;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.persistence.DatabaseTest;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The advisor's tools read through row level security: a model can never see another user's data. */
class AdvisorToolsDatabaseTest extends DatabaseTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Autowired
    AdvisorTools tools;

    @Autowired
    MonthlyPlanRepository plans;

    @Autowired
    RunResultRepository runs;

    @Test
    void toolsSeeOnlyTheCurrentUsersData() {
        plans.save(new MonthlyPlan(NOVEMBER, Distance.ofKilometers(150), List.of()));
        runs.saveAll(List.of(new RunResult("a", LocalDateTime.of(2026, 11, 2, 6, 0),
                Distance.ofKilometers(10), Duration.ofMinutes(55), 150)));

        assertThat(tools.monthSummary("2026-11").actualKm()).isEqualTo(10.0);
        assertThat(tools.plan("2026-11").targetKm()).isEqualTo(150.0);

        currentUser.switchToNewUser();

        assertThat(tools.monthSummary("2026-11").actualKm()).isZero();
        assertThat(tools.runs("2026-11")).isEmpty();
        assertThat(tools.plan("2026-11").targetKm()).isZero();
    }
}
