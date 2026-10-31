package com.example.runbuddy.advisor.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.advisor.Advice;
import com.example.runbuddy.advisor.AdvisorInfo;
import com.example.runbuddy.advisor.AdvisorTools;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import com.example.runbuddy.advisor.ChatTurn;
import com.example.runbuddy.advisor.Consultation;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.WorkoutType;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.ChatOptions;

class SpringAiTrainingAdvisorTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final Consultation ASK_FOR_PLAN = new Consultation(NOVEMBER, List.of(
            ChatTurn.user("今月どうだった？"), ChatTurn.advisor("よく走れています。"), ChatTurn.user("来月のプランを作って")));

    private static final String VALID_PLAN = """
            {"reply": "11月は月150kmを目標にしましょう。",
             "proposedPlan": {"targetKm": 150,
                              "workouts": [{"date": "2026-11-08", "type": "LONG", "distanceKm": 20, "note": "LSD"},
                                           {"date": "2026-11-03", "type": "EASY", "distanceKm": 6, "note": ""}]}}
            """;
    private static final String PLAN_OUTSIDE_MONTH = """
            {"reply": "12月1日にロング走を入れました。",
             "proposedPlan": {"targetKm": 150,
                              "workouts": [{"date": "2026-12-01", "type": "LONG", "distanceKm": 20, "note": ""}]}}
            """;

    @Test
    void validProposalBecomesADomainPlan() {
        ScriptedChatModel model = new ScriptedChatModel(VALID_PLAN);

        Advice advice = advisor(model).consult(ASK_FOR_PLAN, "flash");

        assertThat(advice.reply()).isEqualTo("11月は月150kmを目標にしましょう。");
        MonthlyPlan plan = advice.plan().orElseThrow();
        assertThat(plan.month()).isEqualTo(NOVEMBER);
        assertThat(plan.targetDistance().toKilometers()).isEqualTo(150);
        assertThat(plan.workouts()).extracting(w -> w.date().getDayOfMonth(), w -> w.type())
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3, WorkoutType.EASY), org.assertj.core.groups.Tuple.tuple(8, WorkoutType.LONG));
    }

    @Test
    void answerWithoutAProposalIsJustAReply() {
        Advice advice = advisor(new ScriptedChatModel("""
                {"reply": "よく走れています。", "proposedPlan": null}
                """)).consult(ASK_FOR_PLAN, "flash");

        assertThat(advice.reply()).isEqualTo("よく走れています。");
        assertThat(advice.plan()).isEmpty();
    }

    @Test
    void sendsTheChosenModelTheConversationAndTheSystemPrompt() {
        ScriptedChatModel model = new ScriptedChatModel(VALID_PLAN);

        advisor(model).consult(ASK_FOR_PLAN, "pro");

        var prompt = model.prompts.getFirst();
        assertThat(prompt.getOptions()).extracting(ChatOptions::getModel).isEqualTo("pro");
        assertThat(prompt.getInstructions()).extracting(m -> m.getMessageType())
                .containsExactly(MessageType.SYSTEM, MessageType.USER, MessageType.ASSISTANT, MessageType.USER);
        assertThat(prompt.getInstructions().getFirst().getText())
                .contains("Today is 2026-10-30")
                .contains("planning 2026-11");
    }

    @Test
    void invalidProposalIsSentBackOnceWithTheReason() {
        ScriptedChatModel model = new ScriptedChatModel(PLAN_OUTSIDE_MONTH, VALID_PLAN);

        Advice advice = advisor(model).consult(ASK_FOR_PLAN, "flash");

        assertThat(advice.plan()).isPresent();
        assertThat(model.prompts).hasSize(2);
        assertThat(model.prompts.get(1).getInstructions().getLast().getText())
                .contains("rejected")
                .contains("2026-12-01");
    }

    @Test
    void proposalStillInvalidAfterTheCorrectionIsDropped() {
        ScriptedChatModel model = new ScriptedChatModel(PLAN_OUTSIDE_MONTH, PLAN_OUTSIDE_MONTH);

        Advice advice = advisor(model).consult(ASK_FOR_PLAN, "flash");

        assertThat(advice.plan()).isEmpty();
        assertThat(advice.reply()).isEqualTo("12月1日にロング走を入れました。");
        assertThat(model.prompts).hasSize(2);
    }

    @Test
    void unknownWorkoutTypeIsCorrectedToo() {
        ScriptedChatModel model = new ScriptedChatModel("""
                {"reply": "r", "proposedPlan": {"targetKm": 10,
                 "workouts": [{"date": "2026-11-02", "type": "FARTLEK", "distanceKm": 5, "note": ""}]}}
                """, VALID_PLAN);

        assertThat(advisor(model).consult(ASK_FOR_PLAN, "flash").plan()).isPresent();
        assertThat(model.prompts.get(1).getInstructions().getLast().getText()).contains("FARTLEK");
    }

    @Test
    void unparsableAnswerMeansTheAdvisorIsUnavailable() {
        ScriptedChatModel model = new ScriptedChatModel("I am not JSON");

        assertThatThrownBy(() -> advisor(model).consult(ASK_FOR_PLAN, "flash"))
                .isInstanceOf(AdvisorUnavailableException.class)
                .hasMessageContaining("test");
    }

    private static SpringAiTrainingAdvisor advisor(ScriptedChatModel model) {
        AdvisorTools tools = new AdvisorTools(new InMemoryMonthlyPlanRepository(), new InMemoryRunResultRepository());
        Clock clock = Clock.fixed(Instant.parse("2026-10-30T09:00:00Z"), ZoneOffset.UTC);
        return new SpringAiTrainingAdvisor(
                new AdvisorInfo("test", "Test", List.of("flash", "pro")),
                model,
                name -> ChatOptions.builder().model(name),
                tools,
                clock);
    }

}
