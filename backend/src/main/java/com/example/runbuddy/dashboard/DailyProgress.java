package com.example.runbuddy.dashboard;

import com.example.runbuddy.shared.Distance;
import java.time.LocalDate;

/** Planned and actual distance on one day, plus the month-to-date totals. */
public record DailyProgress(
        LocalDate date,
        Distance planned,
        Distance actual,
        Distance cumulativePlanned,
        Distance cumulativeActual) {
}
