package com.example.runbuddy.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.ActivitySourceUnavailableException.Reason;
import com.example.runbuddy.shared.Distance;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The source is asked at most once per interval, however many screens ask (#40). */
class NewRunCheckerTest {

    private static final Duration INTERVAL = Duration.ofMinutes(3);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-06T03:00:00Z"));
    private final RunResultRepository repository = new InMemoryRunResultRepository();
    private final List<RunResult> inSource = new ArrayList<>();
    private final AtomicInteger asked = new AtomicInteger();
    private Reason failure;

    private final ActivitySource source = days -> {
        asked.incrementAndGet();
        if (failure != null) {
            throw new ActivitySourceUnavailableException(failure, "source said: " + failure, null);
        }
        return List.copyOf(inSource);
    };

    private final List<Object> published = new ArrayList<>();
    private final RunImportService imports = new RunImportService(source, repository, clock, published::add);
    private final NewRunChecker checker = new NewRunChecker(imports, clock, INTERVAL);

    @Test
    void tellsWhichRunsAreNotImportedYet() {
        repository.saveAll(List.of(run("r1", 3)));
        inSource.addAll(List.of(run("r1", 3), run("r2", 5)));

        NewRunChecker.Checked checked = checker.check();

        assertThat(checked.newRuns()).containsExactly(new NewRun("r2", LocalDate.of(2026, 11, 5)));
        assertThat(checked.checkedAt()).isEqualTo(clock.instant());
    }

    @Test
    void asksTheSourceAtMostOncePerInterval() {
        checker.check();
        clock.advance(INTERVAL.minusSeconds(1));
        inSource.add(run("r2", 5));

        assertThat(checker.check().newRuns()).isEmpty();
        assertThat(asked).hasValue(1);

        clock.advance(Duration.ofSeconds(1));

        assertThat(checker.check().newRuns()).extracting(NewRun::activityId).containsExactly("r2");
        assertThat(asked).hasValue(2);
    }

    /** A run just imported must not be offered again for the rest of the interval. */
    @Test
    void anImportForgetsTheAnswer() {
        inSource.add(run("r2", 5));
        assertThat(checker.check().newRuns()).hasSize(1);

        imports.importLatest();
        published.forEach(event -> checker.onImported((RunsImported) event));

        assertThat(checker.check().newRuns()).isEmpty();
    }

    /** Asking again right after a rate limit would only make it worse. */
    @Test
    void aFailureIsKeptForTheIntervalToo() {
        failure = Reason.RATE_LIMITED;

        assertThatThrownBy(checker::check).isInstanceOf(ActivitySourceUnavailableException.class);
        assertThatThrownBy(checker::check).isInstanceOf(ActivitySourceUnavailableException.class);
        assertThat(asked).hasValue(1);

        failure = null;
        clock.advance(INTERVAL);

        assertThat(checker.check().newRuns()).isEmpty();
        assertThat(asked).hasValue(2);
    }

    @Test
    void theIntervalMustBePositive() {
        assertThatThrownBy(() -> new NewRunChecker(imports, clock, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    private static RunResult run(String id, int day) {
        return new RunResult(id, LocalDateTime.of(2026, 11, day, 7, 0), Distance.ofKilometers(8), Duration.ofMinutes(48), 150);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }
}
