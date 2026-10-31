package com.example.runbuddy.advisor.fake;

import com.example.runbuddy.advisor.Advice;
import com.example.runbuddy.advisor.AdvisorInfo;
import com.example.runbuddy.advisor.AdvisorTools;
import com.example.runbuddy.advisor.Consultation;
import com.example.runbuddy.advisor.TrainingAdvisor;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * A rule-based stand-in for an LLM, for development and tests without API keys.
 *
 * <p>It reads data through the same {@link AdvisorTools} the real advisors use, so the whole path
 * from the screen to row level security is exercised. It proposes a plan when the question
 * mentions a plan ("プラン" or "plan"), building 5% on the previous month.
 */
public class FakeTrainingAdvisor implements TrainingAdvisor {

    private static final AdvisorInfo INFO = new AdvisorInfo("fake", "Fake（開発用・ルールベース）", List.of("rule-based"));

    private final AdvisorTools tools;

    public FakeTrainingAdvisor(AdvisorTools tools) {
        this.tools = tools;
    }

    @Override
    public AdvisorInfo info() {
        return INFO;
    }

    @Override
    public Advice consult(Consultation consultation, String model) {
        YearMonth previous = consultation.planMonth().minusMonths(1);
        AdvisorTools.MonthSummary last = tools.monthSummary(previous.toString());
        String rate = last.targetKm() > 0 ? "目標達成率 " + last.achievementRate() : "目標は未設定";
        String review = "%sは%d回、合計%.1f kmを走りました（平均ペース %s、%s）。"
                .formatted(previous, last.runCount(), last.actualKm(), last.averagePace(), rate);

        String question = consultation.latestQuestion().toLowerCase();
        if (!question.contains("プラン") && !question.contains("plan")) {
            return Advice.replyOnly(review + "「プランを作って」と送ると、%sのプラン案を作ります。".formatted(consultation.planMonth()));
        }
        MonthlyPlan plan = propose(consultation.planMonth(), last.actualKm());
        return new Advice(
                review + "%sは5%%増やして月間%.0f kmを目標に、週5回のプランを提案します。"
                        .formatted(consultation.planMonth(), plan.targetDistance().toKilometers()),
                plan);
    }

    private static MonthlyPlan propose(YearMonth month, double lastMonthKm) {
        double target = Math.max(Math.round(lastMonthKm * 1.05), 60);
        List<PlannedWorkout> workouts = new ArrayList<>();
        for (LocalDate day = month.atDay(1); !day.isAfter(month.atEndOfMonth()); day = day.plusDays(1)) {
            switch (day.getDayOfWeek()) {
                case TUESDAY -> workouts.add(workout(day, WorkoutType.INTERVAL, 8, "1000m × 5"));
                case THURSDAY -> workouts.add(workout(day, WorkoutType.TEMPO, 10, ""));
                case SUNDAY -> workouts.add(workout(day, WorkoutType.LONG, 18, ""));
                case MONDAY, FRIDAY -> workouts.add(workout(day, WorkoutType.EASY, 6, ""));
                default -> { }
            }
        }
        return new MonthlyPlan(month, Distance.ofKilometers(target), workouts);
    }

    private static PlannedWorkout workout(LocalDate day, WorkoutType type, double km, String note) {
        return new PlannedWorkout(day, type, Distance.ofKilometers(km), note);
    }

}
