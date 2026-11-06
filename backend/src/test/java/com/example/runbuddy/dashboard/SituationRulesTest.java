package com.example.runbuddy.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.dashboard.Situation.BehindTarget;
import com.example.runbuddy.dashboard.Situation.MonthEnding;
import com.example.runbuddy.dashboard.Situation.NoRunsLately;
import com.example.runbuddy.dashboard.Situation.RaceApproaching;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.race.Race;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** November 2026 has 30 days; the runner aims for 150 km unless a test says otherwise. */
class SituationRulesTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Nested
    class BehindTargetRule {

        @Test
        void reportsHowFarBehindTheRunnerIsForTheDaysThatHavePassed() {
            // 20 days have passed by the 21st, so 100 km was due; 70 km were run.
            var situations = detect(snapshot(day(21)).target(150).ran(70));

            assertThat(situations).containsExactly(new BehindTarget(NOVEMBER, km(30), km(80), 10));
        }

        @Test
        void aSmallShortfallIsNotWorthACard() {
            // 14 km behind is under a tenth of the 150 km target.
            assertThat(detect(snapshot(day(21)).target(150).ran(86))).isEmpty();
        }

        @Test
        void aMonthWithoutATargetHasNothingToFallBehind() {
            assertThat(detect(snapshot(day(21)).ran(0))).isEmpty();
        }

        @Test
        void nobodyIsBehindOnTheFirstDay() {
            assertThat(detect(snapshot(day(1)).target(150).ran(0))).isEmpty();
        }

        @Test
        void aReachedTargetIsNeverBehind() {
            assertThat(detect(snapshot(day(29)).target(150).ran(150))).doesNotHaveAnyElementsOfTypes(BehindTarget.class);
        }

        @Test
        void isNotRaisedWhileTaperingForARace() {
            // Running less before a race is the plan, so the shortfall is not pointed out.
            var situations = detect(snapshot(day(21)).target(150).ran(70).race(day(21).plusDays(14)));

            assertThat(situations).hasOnlyElementsOfType(RaceApproaching.class);
        }
    }

    @Nested
    class MonthEndingRule {

        @Test
        void asksForNextMonthsPlanInTheLastWeek() {
            assertThat(detect(snapshot(day(23)))).containsExactly(new MonthEnding(NOVEMBER.plusMonths(1), 7));
        }

        @Test
        void waitsUntilTheLastWeek() {
            assertThat(detect(snapshot(day(22)))).isEmpty();
        }

        @Test
        void staysQuietWhenNextMonthIsAlreadyPlanned() {
            assertThat(detect(snapshot(day(28)).nextMonthPlanned())).isEmpty();
        }

        @Test
        void countsNoDaysLeftOnTheLastDay() {
            assertThat(detect(snapshot(day(30)))).containsExactly(new MonthEnding(NOVEMBER.plusMonths(1), 0));
        }
    }

    @Nested
    class RaceApproachingRule {

        @Test
        void startsThreeWeeksBeforeTheRace() {
            Race race = race(day(10).plusDays(21));

            assertThat(detect(snapshot(day(10)).race(race))).containsExactly(new RaceApproaching(race, 21));
            assertThat(detect(snapshot(day(9)).race(race))).isEmpty();
        }

        @Test
        void includesRaceDayButNotARaceThatIsOver() {
            Race race = race(day(10));

            assertThat(detect(snapshot(day(10)).race(race))).containsExactly(new RaceApproaching(race, 0));
            assertThat(detect(snapshot(day(11)).race(race))).isEmpty();
        }

        @Test
        void isAboutTheNearestRace() {
            Race near = race(day(15));
            Race far = race(day(20));

            assertThat(detect(snapshot(day(10)).race(far).race(near))).containsExactly(new RaceApproaching(near, 5));
        }
    }

    @Nested
    class NoRunsLatelyRule {

        @Test
        void speaksUpAfterAWeekWithoutRunning() {
            assertThat(detect(snapshot(day(15)).lastRun(day(8)))).containsExactly(new NoRunsLately(day(8), 7));
        }

        @Test
        void sixDaysOffIsJustRest() {
            assertThat(detect(snapshot(day(15)).lastRun(day(9)))).isEmpty();
        }

        @Test
        void saysNothingBeforeTheFirstRunIsImported() {
            assertThat(detect(snapshot(day(15)))).isEmpty();
        }
    }

    @Test
    void putsTheMostPressingSituationFirst() {
        var situations = detect(snapshot(day(25)).target(150).ran(40).lastRun(day(10)));

        assertThat(situations).extracting(Object::getClass)
                .containsExactly(BehindTarget.class, NoRunsLately.class, MonthEnding.class);
    }

    @Test
    void anApproachingRaceComesBeforeEverythingElse() {
        var situations = detect(snapshot(day(25)).lastRun(day(10)).race(day(25).plusDays(10)));

        assertThat(situations).extracting(Object::getClass)
                .containsExactly(RaceApproaching.class, NoRunsLately.class, MonthEnding.class);
    }

    // --- building a snapshot ---

    private static List<Situation> detect(SnapshotBuilder builder) {
        return SituationRules.detect(builder.build());
    }

    private static SnapshotBuilder snapshot(LocalDate today) {
        return new SnapshotBuilder(today);
    }

    private static LocalDate day(int dayOfMonth) {
        return NOVEMBER.atDay(dayOfMonth);
    }

    private static Distance km(double kilometers) {
        return Distance.ofKilometers(kilometers);
    }

    private static Race race(LocalDate date) {
        return Race.plan("Autumn marathon", date, km(42.195), null);
    }

    private static final class SnapshotBuilder {
        private final LocalDate today;
        private double targetKm;
        private double ranKm;
        private boolean nextMonthPlanned;
        private final List<Race> races = new java.util.ArrayList<>();
        private LocalDate lastRun;

        SnapshotBuilder(LocalDate today) {
            this.today = today;
        }

        SnapshotBuilder target(double km) {
            this.targetKm = km;
            return this;
        }

        /** One run on the 1st of the month covering the whole distance, without setting the last run. */
        SnapshotBuilder ran(double km) {
            this.ranKm = km;
            return this;
        }

        SnapshotBuilder nextMonthPlanned() {
            this.nextMonthPlanned = true;
            return this;
        }

        SnapshotBuilder race(Race race) {
            races.add(race);
            return this;
        }

        SnapshotBuilder race(LocalDate date) {
            return race(SituationRulesTest.race(date));
        }

        SnapshotBuilder lastRun(LocalDate date) {
            this.lastRun = date;
            return this;
        }

        TrainingSnapshot build() {
            YearMonth month = YearMonth.from(today);
            List<RunResult> runs = ranKm == 0 ? List.of()
                    : List.of(new RunResult("run-1", month.atDay(1).atTime(6, 0), km(ranKm), Duration.ofMinutes(60), null));
            PlanAchievement achievement = PlanAchievement.of(new MonthlyPlan(month, km(targetKm), List.of()), runs);
            return new TrainingSnapshot(today, achievement, nextMonthPlanned, races, Optional.ofNullable(lastRun));
        }
    }
}
