package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.ActivitySourceUnavailableException.Reason;
import com.example.runbuddy.activity.RunDetailService.Lookup;
import com.example.runbuddy.activity.fit.FitRunDetailReader;
import com.example.runbuddy.activity.fit.SyntheticFit;
import com.example.runbuddy.shared.Distance;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunDetailServiceTest {

    private static final byte[] TWO_LAPS = new SyntheticFit()
            .record(0, 0.0001, 0.0001, 140)
            .lap(0, 1000f, 330f, 150, 160, 85, null, 90, 5)
            .lap(330, 1000f, 320f, 155, 165, 86, null, 91, 2)
            .zip();

    private final RunResultRepository runs = new InMemoryRunResultRepository();
    private final RunFileRepository files = new InMemoryRunFileRepository();
    private final List<String> downloads = new ArrayList<>();
    private Optional<byte[]> onGarmin = Optional.of(TWO_LAPS);
    private RuntimeException failure;

    private final ActivityFileSource source = activityId -> {
        downloads.add(activityId);
        if (failure != null) {
            throw failure;
        }
        return onGarmin;
    };
    private final RunDetailService service = new RunDetailService(runs, files, source, new FitRunDetailReader());

    @BeforeEach
    void storeARun() {
        runs.saveAll(List.of(new RunResult("garmin-1", LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(2),
                Duration.ofSeconds(650), 152)));
    }

    @Test
    void theFirstLookDownloadsAndKeepsTheFileAndLaterOnesReadIt() {
        Lookup first = service.detail("garmin-1");
        Lookup second = service.detail("garmin-1");

        assertThat(first).isInstanceOfSatisfying(Lookup.Found.class, found -> {
            assertThat(found.detail().laps()).hasSize(2);
            assertThat(found.detail().track()).hasSize(1);
        });
        assertThat(second).isEqualTo(first);
        assertThat(downloads).containsExactly("garmin-1");
        assertThat(files.find("garmin-1")).isPresent();
    }

    @Test
    void aRunTheSourceHasNoFileForHasNoDetailAndNothingIsKept() {
        onGarmin = Optional.empty();

        assertThat(service.detail("garmin-1")).isEqualTo(new Lookup.NoFile());
        assertThat(files.find("garmin-1")).isEmpty();
    }

    @Test
    void anUnknownRunIsNotLookedUp() {
        assertThat(service.detail("garmin-404")).isEqualTo(new Lookup.RunNotFound());
        assertThat(downloads).isEmpty();
    }

    @Test
    void aFailingSourceIsReportedAndNothingIsKept() {
        failure = new ActivitySourceUnavailableException(Reason.RATE_LIMITED, "slow down", null);

        assertThatThrownBy(() -> service.detail("garmin-1")).isSameAs(failure);
        assertThat(files.find("garmin-1")).isEmpty();
    }

    @Test
    void anUnreadableDownloadIsNotKept() {
        onGarmin = Optional.of("not a zip".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.detail("garmin-1")).isInstanceOf(UnreadableActivityFileException.class);
        // Kept, it would fail on every later look too; not kept, the next look downloads it again.
        assertThat(files.find("garmin-1")).isEmpty();
    }
}
