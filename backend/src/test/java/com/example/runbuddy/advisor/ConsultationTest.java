package com.example.runbuddy.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultationTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Test
    void theLatestQuestionIsTheLastUserMessage() {
        Consultation consultation = new Consultation(NOVEMBER, List.of(
                ChatTurn.user("今月どうだった？"), ChatTurn.advisor("よく走れています"), ChatTurn.user("来月のプランを作って")));

        assertThat(consultation.latestQuestion()).isEqualTo("来月のプランを作って");
    }

    @Test
    void mustEndWithTheUser() {
        assertThatThrownBy(() -> new Consultation(NOVEMBER, List.of(ChatTurn.user("a"), ChatTurn.advisor("b"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mustNotBeEmpty() {
        assertThatThrownBy(() -> new Consultation(NOVEMBER, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isCappedSoOneRequestCannotGrowWithoutLimit() {
        List<ChatTurn> tooMany = Collections.nCopies(Consultation.MAX_TURNS + 1, ChatTurn.user("hi"));

        assertThatThrownBy(() -> new Consultation(NOVEMBER, tooMany)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankMessagesAreRejected() {
        assertThatThrownBy(() -> ChatTurn.user("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
