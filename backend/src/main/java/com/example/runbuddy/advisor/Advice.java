package com.example.runbuddy.advisor;

import com.example.runbuddy.plan.MonthlyPlan;
import java.util.Objects;
import java.util.Optional;

/**
 * The advisor's answer. A proposed plan has already passed the domain's invariants; it is not
 * saved until the user adopts it.
 */
public record Advice(String reply, MonthlyPlan proposedPlan) {

    public Advice {
        Objects.requireNonNull(reply, "reply");
    }

    public static Advice replyOnly(String reply) {
        return new Advice(reply, null);
    }

    public Optional<MonthlyPlan> plan() {
        return Optional.ofNullable(proposedPlan);
    }
}
