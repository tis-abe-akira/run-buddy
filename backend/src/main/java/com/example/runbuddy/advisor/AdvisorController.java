package com.example.runbuddy.advisor;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.WorkoutType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorController {

    private final AdvisorDirectory directory;
    private final MonthlyPlanRepository plans;
    private final Clock clock;

    public AdvisorController(AdvisorDirectory directory, MonthlyPlanRepository plans, Clock clock) {
        this.directory = directory;
        this.plans = plans;
        this.clock = clock;
    }

    /** The providers and models configured here. The first of each list is the default. */
    @GetMapping("/providers")
    public List<AdvisorInfo> providers() {
        return directory.available();
    }

    /**
     * Asks the chosen provider. Stateless: the client sends the whole conversation each time.
     * A proposed plan is only returned, never saved; adopting it goes through PUT /api/plans.
     *
     * <p>The proposal is returned already merged with the saved plan ({@link MonthlyPlan#revisedFrom}),
     * so what the runner sees is exactly what adopting it saves, whichever provider answered: the days
     * that have passed keep their plan (#31). The saved plan is read after the model has answered, so no
     * transaction is open while it thinks.
     */
    @PostMapping("/consultations")
    public AdviceBody consult(@RequestBody ConsultationBody body) {
        if (body.messages() == null) {
            throw new IllegalArgumentException("messages are required");
        }
        Consultation consultation = new Consultation(
                body.month(), body.messages().stream().map(MessageBody::toTurn).toList());
        Advice advice = directory.consult(body.provider(), body.model(), consultation);
        return AdviceBody.from(advice.reply(), advice.plan().map(this::asRevision));
    }

    private MonthlyPlan asRevision(MonthlyPlan proposal) {
        MonthlyPlan saved = plans.findByMonth(proposal.month()).orElseGet(() -> MonthlyPlan.empty(proposal.month()));
        return saved.revisedFrom(LocalDate.now(clock), proposal);
    }

    public record ConsultationBody(String provider, String model, YearMonth month, List<MessageBody> messages) {
    }

    public record MessageBody(ChatTurn.Speaker speaker, String text) {

        ChatTurn toTurn() {
            if (speaker == null) {
                throw new IllegalArgumentException("each message needs a speaker");
            }
            return new ChatTurn(speaker, text);
        }
    }

    /** proposedPlan is null when the advisor did not propose one. */
    public record AdviceBody(
            String reply,
            @Schema(types = {"object", "null"}, implementation = ProposedPlanBody.class) ProposedPlanBody proposedPlan) {

        static AdviceBody from(String reply, Optional<MonthlyPlan> plan) {
            return new AdviceBody(reply, plan.map(ProposedPlanBody::from).orElse(null));
        }
    }

    /** Same shape as the body of PUT /api/plans/{month}, so the client can send it back unchanged. */
    public record ProposedPlanBody(YearMonth month, double targetKm, List<ProposedWorkoutBody> workouts) {

        static ProposedPlanBody from(MonthlyPlan plan) {
            return new ProposedPlanBody(
                    plan.month(),
                    plan.targetDistance().toKilometers(),
                    plan.workouts().stream()
                            .map(w -> new ProposedWorkoutBody(w.date(), w.type(), w.distance().toKilometers(), w.note()))
                            .toList());
        }
    }

    public record ProposedWorkoutBody(LocalDate date, WorkoutType type, double distanceKm, String note) {
    }
}
