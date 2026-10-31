package com.example.runbuddy.advisor.springai;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.advisor.AdvisorTools;
import com.example.runbuddy.advisor.TrainingAdvisor;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Gemini is offered exactly when an API key is configured. No request is sent. */
class GeminiAdvisorConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GeminiAdvisorConfig.class)
            .withBean(AdvisorTools.class, () -> new AdvisorTools(new InMemoryMonthlyPlanRepository(), new InMemoryRunResultRepository()))
            .withBean(Clock.class, Clock::systemUTC)
            .withPropertyValues("runbuddy.ai.gemini.models=gemini-3.5-flash,gemini-3.1-pro-preview");

    @Test
    void offeredWithAnApiKey() {
        runner.withPropertyValues("runbuddy.ai.gemini.api-key=dummy-key").run(context -> {
            TrainingAdvisor gemini = context.getBean(TrainingAdvisor.class);
            assertThat(gemini.info().id()).isEqualTo("gemini");
            assertThat(gemini.info().models()).containsExactly("gemini-3.5-flash", "gemini-3.1-pro-preview");
        });
    }

    @Test
    void absentWithoutAnApiKey() {
        runner.run(context -> assertThat(context).doesNotHaveBean(TrainingAdvisor.class));
    }

    @Test
    void absentWithABlankApiKey() {
        runner.withPropertyValues("runbuddy.ai.gemini.api-key=").run(context -> assertThat(context).doesNotHaveBean(TrainingAdvisor.class));
    }
}
