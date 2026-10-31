package com.example.runbuddy.activity.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunResult;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DummyActivitySourceTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Test
    void sameMonthAlwaysYieldsTheSameRuns() {
        DummyActivitySource source = sourceAsOf(LocalDate.of(2027, 1, 1));

        assertThat(source.fetchRuns(NOVEMBER)).isEqualTo(source.fetchRuns(NOVEMBER)).isNotEmpty();
    }

    @Test
    void everyRunHasEveryMeasurement() {
        List<RunResult> runs = sourceAsOf(LocalDate.of(2027, 1, 1)).fetchRuns(NOVEMBER);

        assertThat(runs).allSatisfy(run -> assertThat(run.metrics()).hasNoNullFieldsOrProperties());
    }

    @Test
    void noRunsAfterToday() {
        LocalDate today = LocalDate.of(2026, 11, 15);

        List<RunResult> runs = sourceAsOf(today).fetchRuns(NOVEMBER);

        assertThat(runs).isNotEmpty().allSatisfy(run -> assertThat(run.date()).isBeforeOrEqualTo(today));
    }

    @Test
    void runsAlreadyInThePastDoNotChangeAsTimePasses() {
        List<RunResult> midMonth = sourceAsOf(LocalDate.of(2026, 11, 15)).fetchRuns(NOVEMBER);
        List<RunResult> endOfMonth = sourceAsOf(LocalDate.of(2026, 11, 30)).fetchRuns(NOVEMBER);

        assertThat(endOfMonth).startsWith(midMonth.toArray(RunResult[]::new));
    }

    @Test
    void futureMonthHasNoRuns() {
        assertThat(sourceAsOf(LocalDate.of(2026, 10, 30)).fetchRuns(NOVEMBER)).isEmpty();
    }

    private static DummyActivitySource sourceAsOf(LocalDate today) {
        return new DummyActivitySource(
                Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("UTC")));
    }
}
