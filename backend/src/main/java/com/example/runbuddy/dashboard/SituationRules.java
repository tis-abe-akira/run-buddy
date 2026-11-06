package com.example.runbuddy.dashboard;

import com.example.runbuddy.dashboard.Situation.BehindTarget;
import com.example.runbuddy.dashboard.Situation.MonthEnding;
import com.example.runbuddy.dashboard.Situation.NoRunsLately;
import com.example.runbuddy.dashboard.Situation.RaceApproaching;
import com.example.runbuddy.race.Race;
import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds the situations worth acting on today and puts the most pressing first (#27). Deterministic: the
 * same snapshot always gives the same list. When an AI orders the cards (#28), this order is the
 * fallback.
 */
public final class SituationRules {

    /** The weeks before a race when volume comes down (the advisor tapers over the last 2-3 weeks). */
    static final int TAPER_DAYS = 21;
    /** A shortfall smaller than this share of the target is not worth a card. */
    static final double BEHIND_SHARE_OF_TARGET = 0.10;
    /** Days without a run before it is pointed out; a few rest days are normal. */
    static final int NO_RUN_DAYS = 7;
    /** Days left in the month when next month's plan is asked for. */
    static final int MONTH_END_DAYS = 7;

    /** Most pressing first. */
    private static final List<Class<? extends Situation>> ORDER =
            List.of(RaceApproaching.class, BehindTarget.class, NoRunsLately.class, MonthEnding.class);

    private SituationRules() {}

    public static List<Situation> detect(TrainingSnapshot snapshot) {
        List<Situation> found = new ArrayList<>();
        Optional<RaceApproaching> race = raceApproaching(snapshot);
        race.ifPresent(found::add);
        // Running less before a race is the plan, so a shortfall is not pointed out while tapering.
        if (race.isEmpty()) {
            behindTarget(snapshot).ifPresent(found::add);
        }
        noRunsLately(snapshot).ifPresent(found::add);
        monthEnding(snapshot).ifPresent(found::add);
        found.sort(Comparator.comparingInt(situation -> ORDER.indexOf(situation.getClass())));
        return List.copyOf(found);
    }

    private static Optional<RaceApproaching> raceApproaching(TrainingSnapshot snapshot) {
        LocalDate today = snapshot.today();
        return snapshot.races().stream()
                .filter(race -> race.isUpcoming(today) && race.daysFrom(today) <= TAPER_DAYS)
                .min(Comparator.comparing(Race::date))
                .map(race -> new RaceApproaching(race, race.daysFrom(today)));
    }

    private static Optional<BehindTarget> behindTarget(TrainingSnapshot snapshot) {
        PlanAchievement month = snapshot.thisMonth();
        long target = month.targetDistance().meters();
        long actual = month.actualDistance().meters();
        if (target == 0 || actual >= target) {
            return Optional.empty();
        }
        int length = month.month().lengthOfMonth();
        int daysPassed = snapshot.today().getDayOfMonth() - 1;
        long due = Math.round((double) target * daysPassed / length);
        long shortfall = due - actual;
        if (shortfall < target * BEHIND_SHARE_OF_TARGET) {
            return Optional.empty();
        }
        return Optional.of(new BehindTarget(
                month.month(), Distance.ofMeters(shortfall), Distance.ofMeters(target - actual), length - daysPassed));
    }

    private static Optional<NoRunsLately> noRunsLately(TrainingSnapshot snapshot) {
        if (snapshot.lastRunDate().isEmpty()) {
            return Optional.empty();
        }
        LocalDate last = snapshot.lastRunDate().get();
        long days = ChronoUnit.DAYS.between(last, snapshot.today());
        return days >= NO_RUN_DAYS ? Optional.of(new NoRunsLately(last, days)) : Optional.empty();
    }

    private static Optional<MonthEnding> monthEnding(TrainingSnapshot snapshot) {
        LocalDate today = snapshot.today();
        int daysLeft = today.lengthOfMonth() - today.getDayOfMonth();
        if (snapshot.nextMonthPlanned() || daysLeft > MONTH_END_DAYS) {
            return Optional.empty();
        }
        return Optional.of(new MonthEnding(YearMonth.from(today).plusMonths(1), daysLeft));
    }
}
