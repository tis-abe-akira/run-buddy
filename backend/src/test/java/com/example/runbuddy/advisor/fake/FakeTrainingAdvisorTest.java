package com.example.runbuddy.advisor.fake;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.advisor.Advice;
import com.example.runbuddy.advisor.AdvisorTools;
import com.example.runbuddy.advisor.ChatTurn;
import com.example.runbuddy.advisor.Consultation;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class FakeTrainingAdvisorTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    private final InMemoryRunResultRepository runs = new InMemoryRunResultRepository();
    private final FakeTrainingAdvisor advisor =
            new FakeTrainingAdvisor(new AdvisorTools(new InMemoryMonthlyPlanRepository(), runs));

    @Test
    void reviewsThePreviousMonthFromRealData() {
        runs.saveAll(List.of(new RunResult("a", LocalDateTime.of(2026, 10, 4, 6, 0),
                Distance.ofKilometers(100), Duration.ofMinutes(550), 150)));

        Advice advice = advisor.consult(ask("今月どうだった？"), "rule-based");

        assertThat(advice.reply()).contains("2026-10は1回、合計100.0 km").contains("5:30/km");
        assertThat(advice.plan()).isEmpty();
    }

    @Test
    void proposesAPlanFivePercentAboveLastMonth() {
        runs.saveAll(List.of(new RunResult("a", LocalDateTime.of(2026, 10, 4, 6, 0),
                Distance.ofKilometers(200), Duration.ofMinutes(1100), 150)));

        Advice advice = advisor.consult(ask("来月のプランを作って"), "rule-based");

        var plan = advice.plan().orElseThrow();
        assertThat(plan.month()).isEqualTo(NOVEMBER);
        assertThat(plan.targetDistance().toKilometers()).isEqualTo(210);
        assertThat(plan.workouts()).isNotEmpty().allSatisfy(w -> assertThat(YearMonth.from(w.date())).isEqualTo(NOVEMBER));
    }

    private static Consultation ask(String question) {
        return new Consultation(NOVEMBER, List.of(ChatTurn.user(question)));
    }
}
