package com.example.runbuddy.export;

import static com.example.runbuddy.export.DailyLogTest.run;
import static com.example.runbuddy.export.DailyLogTest.workout;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DailyLogCsvTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    @Test
    void writesHeaderEveryDayAndTotals() {
        MonthlyPlan plan = new MonthlyPlan(NOVEMBER, Distance.ofKilometers(200), List.of(
                new PlannedWorkout(NOVEMBER.atDay(1), WorkoutType.LONG, Distance.ofKilometers(20), "LSD, slow"),
                workout(3, WorkoutType.EASY, 5),
                workout(3, WorkoutType.TEMPO, 8)));
        DailyLog log = DailyLog.of(plan, List.of(run("a", 1, 21.04, 125, 148), run("b", 2, 6, 33, null)));

        List<String> lines = List.of(DailyLogCsv.write(log).split("\r\n"));

        assertThat(lines).hasSize(1 + 30 + 1);
        assertThat(lines.get(0)).isEqualTo("﻿日付,曜日,予定_種別,予定_km,予定_メモ,実績_回数,実績_km,実績_時間,実績_ペース(/km),実績_平均心拍");
        assertThat(lines.get(1)).isEqualTo("2026-11-01,日,LONG,20,\"LSD, slow\",1,21.04,2:05:00,5:56,148");
        assertThat(lines.get(2)).isEqualTo("2026-11-02,月,,,,1,6,33:00,5:30,");
        assertThat(lines.get(3)).isEqualTo("2026-11-03,火,EASY+TEMPO,13,,,,,,");
        assertThat(lines.get(4)).isEqualTo("2026-11-04,水,,,,,,,,");
        assertThat(lines.getLast()).isEqualTo("合計,,,33,月間目標 200 km,2,27.04,2:38:00,5:51,");
    }

    @Test
    void noTargetLeavesItOut() {
        String csv = DailyLogCsv.write(DailyLog.of(MonthlyPlan.empty(NOVEMBER), List.of()));

        assertThat(csv.split("\r\n")[31]).isEqualTo("合計,,,0,,0,0,0:00,,");
    }

    @Test
    void formats() {
        assertThat(DailyLogCsv.km(Distance.ofKilometers(219.53))).isEqualTo("219.53");
        assertThat(DailyLogCsv.km(Distance.ofKilometers(10))).isEqualTo("10");
        assertThat(DailyLogCsv.time(Duration.ofSeconds(3923))).isEqualTo("1:05:23");
        assertThat(DailyLogCsv.time(Duration.ofMinutes(55))).isEqualTo("55:00");
        assertThat(DailyLogCsv.pace(Optional.of(Duration.ofSeconds(339)))).isEqualTo("5:39");
        assertThat(DailyLogCsv.pace(Optional.empty())).isEmpty();
    }
}
