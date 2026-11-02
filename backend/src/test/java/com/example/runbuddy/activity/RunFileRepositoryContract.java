package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.shared.Distance;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** What every {@link RunFileRepository} must do (ADR-0012). A file belongs to a stored run. */
public interface RunFileRepositoryContract {

    RunFileRepository files();

    RunResultRepository runs();

    @Test
    default void aSavedFileIsFoundByItsRun() {
        storeRun("garmin-1");
        byte[] zip = "zip bytes".getBytes(StandardCharsets.UTF_8);

        files().save("garmin-1", zip);

        assertThat(files().find("garmin-1")).hasValueSatisfying(found -> assertThat(found).isEqualTo(zip));
    }

    @Test
    default void aRunWithoutAFileHasNone() {
        storeRun("garmin-1");

        assertThat(files().find("garmin-1")).isEmpty();
        assertThat(files().find("unknown")).isEmpty();
    }

    /** Two tabs opening the same run at once both download it; the first copy stays. */
    @Test
    default void savingAgainKeepsTheFirstFile() {
        storeRun("garmin-1");
        byte[] first = "first".getBytes(StandardCharsets.UTF_8);

        files().save("garmin-1", first);
        files().save("garmin-1", "second".getBytes(StandardCharsets.UTF_8));

        assertThat(files().find("garmin-1")).hasValueSatisfying(found -> assertThat(found).isEqualTo(first));
    }

    private void storeRun(String id) {
        runs().saveAll(List.of(new RunResult(id, LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(8),
                Duration.ofMinutes(45), 150)));
    }
}
