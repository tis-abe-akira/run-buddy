package com.example.runbuddy.dashboard;

import com.example.runbuddy.dashboard.Situation.BehindTarget;
import com.example.runbuddy.dashboard.Situation.MonthEnding;
import com.example.runbuddy.dashboard.Situation.NoRunsLately;
import com.example.runbuddy.dashboard.Situation.RaceApproaching;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What is worth acting on today, most pressing first (#26, #27). Each card says what it is ({@code kind})
 * and carries the numbers to show; the wording lives in the frontend. Distances are in kilometers.
 */
@RestController
public class NextStepsController {

    private final TrainingSnapshots snapshots;

    public NextStepsController(TrainingSnapshots snapshots) {
        this.snapshots = snapshots;
    }

    @GetMapping("/api/next-steps")
    public NextStepsResponse nextSteps() {
        return new NextStepsResponse(SituationRules.detect(snapshots.today()).stream().map(NextStepsController::card).toList());
    }

    private static NextStepCard card(Situation situation) {
        return switch (situation) {
            case RaceApproaching s -> new RaceApproachingCard(
                    s.race().id(), s.race().name(), s.race().date(), s.race().distance().toKilometers(), s.daysToGo());
            case BehindTarget s -> new BehindTargetCard(
                    s.month(), s.shortfall().toKilometers(), s.remaining().toKilometers(), s.daysLeft());
            case NoRunsLately s -> new NoRunsLatelyCard(s.lastRunDate(), s.daysSince());
            case MonthEnding s -> new MonthEndingCard(s.nextMonth(), s.daysLeft());
        };
    }

    public record NextStepsResponse(List<NextStepCard> cards) {}

    @Schema(oneOf = {RaceApproachingCard.class, BehindTargetCard.class, NoRunsLatelyCard.class, MonthEndingCard.class})
    public sealed interface NextStepCard {
        String kind();
    }

    public record RaceApproachingCard(
            @Schema(requiredMode = RequiredMode.REQUIRED, allowableValues = "RACE_APPROACHING") String kind,
            UUID raceId,
            String raceName,
            LocalDate raceDate,
            double distanceKm,
            @Schema(description = "0 on race day") long daysToGo) implements NextStepCard {

        RaceApproachingCard(UUID raceId, String raceName, LocalDate raceDate, double distanceKm, long daysToGo) {
            this("RACE_APPROACHING", raceId, raceName, raceDate, distanceKm, daysToGo);
        }
    }

    public record BehindTargetCard(
            @Schema(requiredMode = RequiredMode.REQUIRED, allowableValues = "BEHIND_TARGET") String kind,
            YearMonth month,
            @Schema(description = "how far behind the pace that reaches the target") double shortfallKm,
            @Schema(description = "still to run to reach the target") double remainingKm,
            @Schema(description = "days left in the month, today included") int daysLeft) implements NextStepCard {

        BehindTargetCard(YearMonth month, double shortfallKm, double remainingKm, int daysLeft) {
            this("BEHIND_TARGET", month, shortfallKm, remainingKm, daysLeft);
        }
    }

    public record NoRunsLatelyCard(
            @Schema(requiredMode = RequiredMode.REQUIRED, allowableValues = "NO_RUNS_LATELY") String kind,
            LocalDate lastRunDate,
            long daysSince) implements NextStepCard {

        NoRunsLatelyCard(LocalDate lastRunDate, long daysSince) {
            this("NO_RUNS_LATELY", lastRunDate, daysSince);
        }
    }

    public record MonthEndingCard(
            @Schema(requiredMode = RequiredMode.REQUIRED, allowableValues = "MONTH_ENDING") String kind,
            YearMonth nextMonth,
            @Schema(description = "days left in this month after today; 0 on its last day") int daysLeft)
            implements NextStepCard {

        MonthEndingCard(YearMonth nextMonth, int daysLeft) {
            this("MONTH_ENDING", nextMonth, daysLeft);
        }
    }
}
