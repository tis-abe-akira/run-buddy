package com.example.runbuddy.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    @Test
    void aMonthIsItsFirstToItsLastDay() {
        DateRange february = DateRange.of(YearMonth.of(2028, 2));

        assertThat(february.first()).isEqualTo(LocalDate.of(2028, 2, 1));
        assertThat(february.last()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void containsBothEnds() {
        DateRange range = new DateRange(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 2));

        assertThat(range.contains(LocalDate.of(2026, 10, 31))).isTrue();
        assertThat(range.contains(LocalDate.of(2026, 11, 2))).isTrue();
        assertThat(range.contains(LocalDate.of(2026, 10, 30))).isFalse();
        assertThat(range.contains(LocalDate.of(2026, 11, 3))).isFalse();
    }

    @Test
    void oneDayIsARange() {
        LocalDate day = LocalDate.of(2026, 11, 2);

        assertThat(new DateRange(day, day).contains(day)).isTrue();
    }

    @Test
    void listsTheMonthsItTouches() {
        DateRange range = new DateRange(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 2, 1));

        assertThat(range.months()).containsExactly(YearMonth.of(2026, 12), YearMonth.of(2027, 1), YearMonth.of(2027, 2));
    }

    @Test
    void theEndCannotComeBeforeTheStart() {
        assertThatThrownBy(() -> new DateRange(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsAsFirstToLast() {
        assertThat(new DateRange(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 2))).hasToString("2026-10-31..2026-11-02");
    }
}
