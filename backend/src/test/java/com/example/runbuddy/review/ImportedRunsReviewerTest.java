package com.example.runbuddy.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.ActivityFileReader;
import com.example.runbuddy.activity.InMemoryRunFileRepository;
import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.activity.RunsImported;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import com.example.runbuddy.shared.Distance;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

/** The automatic one-liner on import (#36). */
class ImportedRunsReviewerTest {

    private static final Instant NOW = Instant.parse("2026-11-02T09:00:00Z");
    private static final Executor AT_ONCE = Runnable::run;

    private final RunResultRepository runs = new InMemoryRunResultRepository();
    private final RunReviewRepository reviews = new InMemoryRunReviewRepository();
    private final List<String> asked = new ArrayList<>();

    @Test
    void offByDefaultTheAiIsNotCalled() {
        RunResult run = store("r1");

        new ImportedRunsReviewer(service(reviewer(null)), false, AT_ONCE).onImported(new RunsImported(List.of(run)));

        assertThat(asked).isEmpty();
        assertThat(reviews.find("r1")).isEmpty();
    }

    @Test
    void whenOnEachNewRunGetsAOneLiner() {
        RunResult first = store("r1");
        RunResult second = store("r2");

        new ImportedRunsReviewer(service(reviewer(null)), true, AT_ONCE).onImported(new RunsImported(List.of(first, second)));

        assertThat(asked).containsExactly("r1", "r2");
        assertThat(reviews.find("r1")).hasValueSatisfying(review -> {
            assertThat(review.isBrief()).isTrue();
            assertThat(review.headline()).isEqualTo("一言 r1");
        });
        assertThat(reviews.find("r2")).hasValueSatisfying(review -> assertThat(review.isBrief()).isTrue());
    }

    /** The import has returned already; a failure only means that run has no one-liner. */
    @Test
    void aFailingAiLeavesThatRunWithoutAOneLinerAndGoesOn() {
        RunResult failing = store("fails");
        RunResult fine = store("r2");

        new ImportedRunsReviewer(service(reviewer("fails")), true, AT_ONCE).onImported(new RunsImported(List.of(failing, fine)));

        assertThat(reviews.find("fails")).isEmpty();
        assertThat(reviews.find("r2")).isPresent();
    }

    @Test
    void theReviewsAreWrittenOffTheImportThread() {
        RunResult run = store("r1");
        List<Runnable> queued = new ArrayList<>();

        new ImportedRunsReviewer(service(reviewer(null)), true, queued::add).onImported(new RunsImported(List.of(run)));

        assertThat(asked).isEmpty();
        queued.forEach(Runnable::run);
        assertThat(asked).containsExactly("r1");
    }

    private RunResult store(String id) {
        RunResult run = new RunResult(id, LocalDateTime.of(2026, 11, 1, 6, 0), Distance.ofKilometers(10), Duration.ofMinutes(60), 150);
        runs.saveAll(List.of(run));
        return run;
    }

    private RunReviewService service(RunReviewer reviewer) {
        ActivityFileReader neverRead = zip -> {
            throw new AssertionError("a one-liner never reads the activity file");
        };
        return new RunReviewService(runs, new InMemoryRunFileRepository(), neverRead, reviews, List.of(reviewer),
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(5));
    }

    /** Writes "一言 <id>", and fails for the run with {@code failFor}. */
    private RunReviewer reviewer(String failFor) {
        return new RunReviewer() {
            @Override
            public String id() {
                return "gemini";
            }

            @Override
            public String model() {
                return "flash";
            }

            @Override
            public Written review(RunReviewRequest request) {
                throw new AssertionError("the detailed review is only written on request");
            }

            @Override
            public String headline(RunReviewRequest request) {
                String id = request.run().activityId();
                asked.add(id);
                if (id.equals(failFor)) {
                    throw new AdvisorUnavailableException("gemini could not answer", null);
                }
                return "一言 " + id;
            }
        };
    }
}
