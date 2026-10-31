package com.example.runbuddy.export;

import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.shared.Distance;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/** Writes a {@link DailyLog} as CSV: a header, one line per day, then a 合計 line. */
final class DailyLogCsv {

    static final List<String> HEADER = List.of(
            "日付", "曜日",
            "予定_種別", "予定_km", "予定_メモ",
            "実績_回数", "実績_km", "実績_時間", "実績_ペース(/km)", "実績_平均心拍");

    private DailyLogCsv() {
    }

    static String write(DailyLog log) {
        Csv csv = new Csv().row(HEADER);
        for (DailyLog.Day day : log.days()) {
            csv.row(List.of(
                    day.date().toString(),
                    day.date().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.JAPANESE),
                    day.workouts().stream().map(w -> w.type().name()).collect(Collectors.joining("+")),
                    day.workouts().isEmpty() ? "" : km(day.plannedDistance()),
                    day.workouts().stream().map(PlannedWorkout::note).filter(n -> !n.isBlank()).collect(Collectors.joining(" / ")),
                    day.runs().isEmpty() ? "" : String.valueOf(day.runs().size()),
                    day.runs().isEmpty() ? "" : km(day.actualDistance()),
                    day.runs().isEmpty() ? "" : time(day.totalTime()),
                    pace(day.pace()),
                    heartRate(day.averageHeartRate())));
        }
        String target = log.targetDistance().isZero() ? "" : "月間目標 " + km(log.targetDistance()) + " km";
        csv.row(List.of(
                "合計", "",
                "", km(log.plannedDistance()), target,
                String.valueOf(log.runCount()), km(log.actualDistance()), time(log.totalTime()), pace(log.averagePace()), ""));
        return csv.toString();
    }

    /** "10", "10.5", "219.53": plain decimal kilometers, no grouping, so spreadsheets read numbers. */
    static String km(Distance distance) {
        return BigDecimal.valueOf(distance.meters(), 3).stripTrailingZeros().toPlainString();
    }

    /** "55:00", "1:05:23" */
    static String time(Duration duration) {
        long seconds = duration.toSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours > 0
                ? "%d:%02d:%02d".formatted(hours, minutes, seconds % 60)
                : "%d:%02d".formatted(minutes, seconds % 60);
    }

    static String pace(Optional<Duration> pace) {
        return pace.map(p -> "%d:%02d".formatted(p.toSeconds() / 60, p.toSeconds() % 60)).orElse("");
    }

    static String heartRate(OptionalLong heartRate) {
        return heartRate.isPresent() ? String.valueOf(heartRate.getAsLong()) : "";
    }
}
