package com.example.runbuddy.advisor.springai;

import com.example.runbuddy.advisor.Advice;
import com.example.runbuddy.advisor.AdvisorInfo;
import com.example.runbuddy.advisor.AdvisorTools;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import com.example.runbuddy.advisor.ChatTurn;
import com.example.runbuddy.advisor.Consultation;
import com.example.runbuddy.advisor.TrainingAdvisor;
import com.example.runbuddy.plan.MonthlyPlan;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * A {@link TrainingAdvisor} for any provider Spring AI supports. Provider-specific setup (client,
 * credentials, options) stays in that provider's configuration class; this class only uses the
 * features every provider has: chat, tool calling and structured output.
 */
public class SpringAiTrainingAdvisor implements TrainingAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SpringAiTrainingAdvisor.class);

    private static final String SYSTEM_PROMPT = """
            You are a running coach helping one runner plan their monthly training.
            Today is %s. The runner is planning %s.

            Calendar of %s (use it for every weekday; do not work weekdays out yourself):
            %s

            Before giving advice, look at their data with the tools: the summary, runs and plan of
            recent months, and the plan they already have for %s.

            Answer in the language the runner writes in. Keep the reply short and concrete.
            Write the reply as plain text: no Markdown (no **, *, # or tables); use line breaks and
            numbered lines like "1." instead. Quote numbers exactly as the tools return them, and do
            not round a missed target up to "achieved".

            Put a training plan in proposedPlan only when the runner asks for one or agrees to one;
            otherwise set proposedPlan to null. A proposed plan must:
            - only use dates in %s, on the weekdays the runner asked for,
            - use type EASY, TEMPO, INTERVAL, LONG or RACE,
            - give distances in kilometers, and targetKm as the monthly distance goal,
            - build up gradually from what the runner actually ran recently.
            Describe the plan in the reply as well.
            """;

    private final AdvisorInfo info;
    private final ChatClient chatClient;
    private final Function<String, ChatOptions.Builder<?>> optionsForModel;
    private final AdvisorTools tools;
    private final Clock clock;
    private final Predicate<Throwable> isRateLimit;

    /**
     * @param optionsForModel builds the provider's own options for a model id, so provider-specific
     *     settings (thinking, safety...) can be added by its configuration
     * @param isRateLimit recognises the provider's "quota exceeded" error, which each SDK reports
     *     in its own way
     */
    public SpringAiTrainingAdvisor(
            AdvisorInfo info,
            ChatModel chatModel,
            Function<String, ChatOptions.Builder<?>> optionsForModel,
            Predicate<Throwable> isRateLimit,
            AdvisorTools tools,
            Clock clock) {
        this.info = info;
        this.chatClient = ChatClient.create(chatModel);
        this.optionsForModel = optionsForModel;
        this.isRateLimit = isRateLimit;
        this.tools = tools;
        this.clock = clock;
    }

    @Override
    public AdvisorInfo info() {
        return info;
    }

    @Override
    public Advice consult(Consultation consultation, String model) {
        List<Message> messages = toMessages(consultation.turns());
        AdvisorAnswer answer = ask(consultation, model, messages);
        if (answer.proposedPlan() == null) {
            return Advice.replyOnly(answer.reply());
        }
        try {
            return new Advice(answer.reply(), answer.toPlan(consultation.planMonth()));
        } catch (IllegalArgumentException invalid) {
            return retryWithCorrection(consultation, model, messages, answer, invalid);
        }
    }

    /** Models get structured output wrong now and then; one correction usually fixes it. */
    private Advice retryWithCorrection(
            Consultation consultation, String model, List<Message> messages, AdvisorAnswer first, RuntimeException invalid) {
        List<Message> corrected = new ArrayList<>(messages);
        corrected.add(new AssistantMessage(first.reply()));
        corrected.add(new UserMessage("""
                The proposed plan was rejected: %s
                Send the corrected plan, keeping everything else the same.""".formatted(invalid.getMessage())));
        AdvisorAnswer second = ask(consultation, model, corrected);
        if (second.proposedPlan() == null) {
            return Advice.replyOnly(first.reply());
        }
        try {
            MonthlyPlan plan = second.toPlan(consultation.planMonth());
            return new Advice(second.reply(), plan);
        } catch (IllegalArgumentException stillInvalid) {
            return Advice.replyOnly(first.reply());
        }
    }

    private AdvisorAnswer ask(Consultation consultation, String model, List<Message> messages) {
        String month = consultation.planMonth().toString();
        try {
            AdvisorAnswer answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT.formatted(
                            LocalDate.now(clock), month, month, calendar(consultation.planMonth()), month, month))
                    .messages(messages)
                    .tools(tools)
                    .options(optionsForModel.apply(model))
                    .call()
                    .entity(AdvisorAnswer.class);
            if (answer == null || answer.reply() == null || answer.reply().isBlank()) {
                throw new AdvisorUnavailableException(info.id() + " returned an empty answer", null);
            }
            return answer;
        } catch (AdvisorUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            if (causes(e).anyMatch(isRateLimit)) {
                log.warn("{} ({}) rate limit or quota exceeded", info.id(), model);
                throw new AdvisorUnavailableException(
                        AdvisorUnavailableException.Reason.RATE_LIMITED, info.displayName() + " rate limit or quota exceeded", e);
            }
            log.warn("{} ({}) could not answer", info.id(), model, e);
            throw new AdvisorUnavailableException(info.id() + " could not answer: " + e.getMessage(), e);
        }
    }

    private static Stream<Throwable> causes(Throwable e) {
        return Stream.iterate(e, Objects::nonNull, Throwable::getCause).limit(10);
    }

    /** "Sun: 1, 8, 15, 22, 29" and so on, one line per weekday starting from Monday. */
    static String calendar(YearMonth month) {
        Map<DayOfWeek, List<String>> daysByWeekday = new EnumMap<>(DayOfWeek.class);
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            daysByWeekday.computeIfAbsent(month.atDay(day).getDayOfWeek(), d -> new ArrayList<>()).add(String.valueOf(day));
        }
        return daysByWeekday.entrySet().stream()
                .map(e -> e.getKey().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ": " + String.join(", ", e.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private static List<Message> toMessages(List<ChatTurn> turns) {
        return turns.stream()
                .<Message>map(turn -> switch (turn.speaker()) {
                    case USER -> new UserMessage(turn.text());
                    case ADVISOR -> new AssistantMessage(turn.text());
                })
                .toList();
    }
}
