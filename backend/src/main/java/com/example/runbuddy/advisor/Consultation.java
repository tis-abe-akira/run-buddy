package com.example.runbuddy.advisor;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * A conversation with the advisor about planning one month.
 *
 * <p>The whole history travels with every request; the server keeps no conversation state, which
 * is what lets the user switch provider or model at any turn.
 *
 * @param planMonth the month a proposed plan would be for
 */
public record Consultation(YearMonth planMonth, List<ChatTurn> turns) {

    public static final int MAX_TURNS = 40;

    public Consultation {
        Objects.requireNonNull(planMonth, "planMonth");
        turns = List.copyOf(Objects.requireNonNull(turns, "turns"));
        if (turns.isEmpty() || turns.getLast().speaker() != ChatTurn.Speaker.USER) {
            throw new IllegalArgumentException("a consultation must end with the user's message");
        }
        if (turns.size() > MAX_TURNS) {
            throw new IllegalArgumentException("a consultation can have at most " + MAX_TURNS + " messages");
        }
    }

    public String latestQuestion() {
        return turns.getLast().text();
    }
}
