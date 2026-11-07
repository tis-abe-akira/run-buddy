package com.example.runbuddy.dashboard;

import com.example.runbuddy.race.Race;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What {@link SituationRules} looks at, as of {@code today}. "Today" is a value, not the clock, so the
 * rules can be tested on any day.
 *
 * @param thisMonth        today's month: its plan and its runs
 * @param nextMonthPlanned whether next month already has a plan (a target or a workout)
 * @param races            every race, in any order
 * @param lastRunDate      the day of the most recent stored run; empty before the first import
 */
public record TrainingSnapshot(
        LocalDate today, PlanAchievement thisMonth, boolean nextMonthPlanned, List<Race> races, Optional<LocalDate> lastRunDate) {

    public TrainingSnapshot {
        Objects.requireNonNull(today, "today");
        Objects.requireNonNull(thisMonth, "thisMonth");
        Objects.requireNonNull(lastRunDate, "lastRunDate");
        races = List.copyOf(races);
        if (!thisMonth.month().equals(YearMonth.from(today))) {
            throw new IllegalArgumentException("thisMonth is " + thisMonth.month() + " but today is " + today);
        }
    }
}
