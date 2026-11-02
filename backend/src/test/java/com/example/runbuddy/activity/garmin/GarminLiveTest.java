package com.example.runbuddy.activity.garmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.ActivitySourceUnavailableException.Reason;
import com.example.runbuddy.activity.ActivitySourceUnavailableException;
import com.example.runbuddy.activity.RunDetail;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.fit.FitRunDetailReader;
import com.example.runbuddy.shared.DateRange;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Calls the real Garmin Connect Personal API with the owner's key: token exchange, the activity
 * search and our reading of its response. Runs only when GARMIN_API_KEY is set in the OS
 * environment. Read-only; prints nothing about the runs.
 */
@EnabledIfEnvironmentVariable(named = "GARMIN_API_KEY", matches = ".+")
class GarminLiveTest {

    private static final String BASE_URL = "https://personal.connectapi.garmin.com";

    @Test
    void fetchesLastMonthsRuns() {
        GarminActivitySource source = GarminActivitySource.connect(BASE_URL, System.getenv("GARMIN_API_KEY"), Clock.systemDefaultZone());
        YearMonth lastMonth = YearMonth.now().minusMonths(1);

        List<RunResult> runs = source.fetchRuns(DateRange.of(lastMonth));
        // A second month reuses the token.
        source.fetchRuns(DateRange.of(lastMonth.minusMonths(1)));

        assertThat(runs).allSatisfy(run -> {
            assertThat(run.activityId()).startsWith("garmin-");
            assertThat(YearMonth.from(run.date())).isEqualTo(lastMonth);
        });
        assertThat(runs).extracting(RunResult::activityId).doesNotHaveDuplicates();
    }

    @Test
    void aWrongKeyIsReportedAsRejected() {
        GarminActivitySource source = GarminActivitySource.connect(BASE_URL, "not-a-real-key", Clock.systemDefaultZone());

        assertThatThrownBy(() -> source.fetchRuns(DateRange.of(YearMonth.now())))
                .isInstanceOfSatisfying(ActivitySourceUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(Reason.CREDENTIALS_REJECTED));
    }

    /** ADR-0012: the latest run's activity file downloads and has laps; nothing about the run is printed. */
    @Test
    void downloadsAndReadsTheLatestRunsActivityFile() {
        GarminActivitySource source = GarminActivitySource.connect(BASE_URL, System.getenv("GARMIN_API_KEY"), Clock.systemDefaultZone());
        YearMonth thisMonth = YearMonth.now();
        List<RunResult> runs = source.fetchRuns(new DateRange(thisMonth.minusMonths(1).atDay(1), LocalDate.now()));
        assertThat(runs).isNotEmpty();

        byte[] zip = source.fetchFitZip(runs.getLast().activityId()).orElseThrow();
        RunDetail detail = new FitRunDetailReader().read(zip);

        assertThat(detail.laps()).isNotEmpty();
    }
}
