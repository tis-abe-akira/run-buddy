package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunImportServiceTest {

    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    private final RunResultRepository repository = new InMemoryRunResultRepository();

    @Test
    void importStoresFetchedRuns() {
        ActivitySource source = month -> List.of(run("r1", 3), run("r2", 5));

        var result = new RunImportService(source, repository).importRuns(NOVEMBER);

        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.added()).isEqualTo(2);
        assertThat(repository.findByMonth(NOVEMBER)).extracting(RunResult::activityId).containsExactly("r1", "r2");
    }

    @Test
    void importingTwiceDoesNotDuplicateRuns() {
        ActivitySource source = month -> List.of(run("r1", 3));
        RunImportService service = new RunImportService(source, repository);

        service.importRuns(NOVEMBER);
        var second = service.importRuns(NOVEMBER);

        assertThat(second.added()).isZero();
        assertThat(repository.findByMonth(NOVEMBER)).hasSize(1);
    }

    private static RunResult run(String id, int day) {
        return new RunResult(
                id, LocalDateTime.of(2026, 11, day, 7, 0), Distance.ofKilometers(8), Duration.ofMinutes(48), 150);
    }
}
