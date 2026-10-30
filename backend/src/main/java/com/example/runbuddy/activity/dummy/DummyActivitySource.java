package com.example.runbuddy.activity.dummy;

import com.example.runbuddy.activity.ActivitySource;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.shared.Distance;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stands in for Garmin in Phase 1 (ADR-0004).
 *
 * <p>Runs are generated from a seed derived from the month, so the same month always yields the
 * same runs. Days after "today" never have runs.
 */
@Component
@ConditionalOnProperty(name = "runbuddy.activity-source", havingValue = "dummy", matchIfMissing = true)
public class DummyActivitySource implements ActivitySource {

    private static final double RUN_PROBABILITY = 0.6;

    private final Clock clock;

    public DummyActivitySource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<RunResult> fetchRuns(YearMonth month) {
        Random random = new Random(month.getYear() * 100L + month.getMonthValue());
        LocalDate today = LocalDate.now(clock);
        List<RunResult> runs = new ArrayList<>();
        for (LocalDate day = month.atDay(1); !day.isAfter(month.atEndOfMonth()); day = day.plusDays(1)) {
            // Draw for every day, even ones we skip, so earlier days do not change as "today" moves.
            boolean runsToday = random.nextDouble() < RUN_PROBABILITY;
            RunResult run = generateRun(day, random);
            if (runsToday && !day.isAfter(today)) {
                runs.add(run);
            }
        }
        return runs;
    }

    private static RunResult generateRun(LocalDate day, Random random) {
        boolean longRun = day.getDayOfWeek() == DayOfWeek.SUNDAY;
        double km = longRun ? 15 + random.nextDouble() * 7 : 5 + random.nextDouble() * 7;
        Distance distance = Distance.ofKilometers(Math.round(km * 100) / 100.0);
        int paceSecondsPerKm = 300 + random.nextInt(91);
        Duration duration = Duration.ofSeconds(Math.round(paceSecondsPerKm * distance.toKilometers()));
        int heartRate = 135 + random.nextInt(31);
        LocalTime start = LocalTime.of(6, 0).plusMinutes(random.nextInt(90));
        return new RunResult("dummy-" + day, day.atTime(start), distance, duration, heartRate);
    }
}
