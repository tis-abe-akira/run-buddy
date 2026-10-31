package com.example.runbuddy.advisor.springai;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.advisor.Advice;
import com.example.runbuddy.advisor.AdvisorDirectory;
import com.example.runbuddy.advisor.ChatTurn;
import com.example.runbuddy.advisor.Consultation;
import com.example.runbuddy.activity.RunImportService;
import com.example.runbuddy.persistence.DatabaseTest;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Calls the real Gemini API: tool calling against the database, structured output and the
 * domain checks together. Runs only when GEMINI_API_KEY is set, and costs a little each run.
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiLiveTest extends DatabaseTest {

    @Autowired
    AdvisorDirectory advisors;

    @Autowired
    RunImportService imports;

    @Test
    void proposesAValidPlanFromTheRunnersData() {
        imports.importRuns(YearMonth.of(2026, 9));
        YearMonth october = YearMonth.of(2026, 10);
        Consultation consultation = new Consultation(october, List.of(
                ChatTurn.user("2026年9月の走りを振り返って、10月のプランを作ってください。")));
        String model = advisors.available().stream()
                .filter(info -> info.id().equals("gemini")).findFirst().orElseThrow().models().getFirst();

        Advice advice = advisors.consult("gemini", model, consultation);

        assertThat(advice.reply()).isNotBlank();
        assertThat(advice.plan()).hasValueSatisfying(plan -> {
            assertThat(plan.month()).isEqualTo(october);
            assertThat(plan.workouts()).isNotEmpty();
        });
    }
}
