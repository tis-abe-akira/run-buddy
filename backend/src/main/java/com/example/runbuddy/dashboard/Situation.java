package com.example.runbuddy.dashboard;

import com.example.runbuddy.race.Race;
import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Something about the runner's training that is worth acting on today (#26). Situations are found by
 * {@link SituationRules} from data the app already has; how they are shown is decided elsewhere.
 */
public sealed interface Situation {

    /**
     * A target race is close enough to taper for it.
     *
     * @param daysToGo 0 on race day
     */
    record RaceApproaching(Race race, long daysToGo) implements Situation {
        public RaceApproaching {
            Objects.requireNonNull(race, "race");
            if (daysToGo < 0) {
                throw new IllegalArgumentException("the race is over: " + daysToGo);
            }
        }
    }

    /**
     * Fewer kilometers than the days that have passed call for.
     *
     * @param shortfall how far behind the pace that reaches the target
     * @param remaining what is still to run to reach the target
     * @param daysLeft  days left in the month, today included
     */
    record BehindTarget(YearMonth month, Distance shortfall, Distance remaining, int daysLeft) implements Situation {
        public BehindTarget {
            Objects.requireNonNull(month, "month");
            Objects.requireNonNull(shortfall, "shortfall");
            Objects.requireNonNull(remaining, "remaining");
            if (shortfall.isZero() || remaining.isZero()) {
                throw new IllegalArgumentException("not behind: shortfall " + shortfall + ", remaining " + remaining);
            }
            if (daysLeft < 1) {
                throw new IllegalArgumentException("the month is over: " + daysLeft);
            }
        }
    }

    /** No run for a while (since {@code lastRunDate}). */
    record NoRunsLately(LocalDate lastRunDate, long daysSince) implements Situation {
        public NoRunsLately {
            Objects.requireNonNull(lastRunDate, "lastRunDate");
            if (daysSince < 1) {
                throw new IllegalArgumentException("ran today: " + daysSince);
            }
        }
    }

    /**
     * The month is ending and the next one has no plan yet.
     *
     * @param daysLeft days left in this month after today (0 on its last day)
     */
    record MonthEnding(YearMonth nextMonth, int daysLeft) implements Situation {
        public MonthEnding {
            Objects.requireNonNull(nextMonth, "nextMonth");
            if (daysLeft < 0) {
                throw new IllegalArgumentException("negative days left: " + daysLeft);
            }
        }
    }
}
