package com.example.runbuddy.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdvisorDirectoryTest {

    private static final Consultation QUESTION = new Consultation(YearMonth.of(2026, 11), List.of(ChatTurn.user("hi")));

    private final AdvisorDirectory directory = new AdvisorDirectory(List.of(
            new EchoAdvisor("gemini", List.of("flash", "pro")), new EchoAdvisor("claude", List.of("opus"))));

    @Test
    void listsConfiguredAdvisorsInOrder() {
        assertThat(directory.available()).extracting(AdvisorInfo::id).containsExactly("gemini", "claude");
    }

    @Test
    void routesToTheChosenProviderAndModel() {
        assertThat(directory.consult("gemini", "pro", QUESTION).reply()).isEqualTo("gemini/pro: hi");
        assertThat(directory.consult("claude", "opus", QUESTION).reply()).isEqualTo("claude/opus: hi");
    }

    @Test
    void unknownProviderIsRejected() {
        assertThatThrownBy(() -> directory.consult("openai", "gpt", QUESTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openai");
    }

    @Test
    void modelTheProviderDoesNotOfferIsRejected() {
        assertThatThrownBy(() -> directory.consult("gemini", "opus", QUESTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("opus");
    }

    @Test
    void duplicateIdsAreAConfigurationError() {
        List<TrainingAdvisor> twice = List.of(new EchoAdvisor("gemini", List.of("a")), new EchoAdvisor("gemini", List.of("b")));

        assertThatThrownBy(() -> new AdvisorDirectory(twice)).isInstanceOf(IllegalStateException.class);
    }

    /** Answers with the provider, model and question it was given. */
    record EchoAdvisor(String id, List<String> models) implements TrainingAdvisor {

        @Override
        public AdvisorInfo info() {
            return new AdvisorInfo(id, id, models);
        }

        @Override
        public Advice consult(Consultation consultation, String model) {
            return Advice.replyOnly(id + "/" + model + ": " + consultation.latestQuestion());
        }
    }
}
