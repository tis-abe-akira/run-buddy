package com.example.runbuddy.shared;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Calendar days from {@code first} to {@code last}, both included. */
public record DateRange(LocalDate first, LocalDate last) {

    public DateRange {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(last, "last");
        if (last.isBefore(first)) {
            throw new IllegalArgumentException("a range cannot end (" + last + ") before it starts (" + first + ")");
        }
    }

    public static DateRange of(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }

    public boolean contains(LocalDate day) {
        return !day.isBefore(first) && !day.isAfter(last);
    }

    /** Every month with at least one day in the range, earliest first. */
    public List<YearMonth> months() {
        YearMonth end = YearMonth.from(last);
        return Stream.iterate(YearMonth.from(first), month -> !month.isAfter(end), month -> month.plusMonths(1)).toList();
    }

    @Override
    public String toString() {
        return first + ".." + last;
    }
}
