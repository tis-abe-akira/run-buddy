package com.example.runbuddy.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.runbuddy.activity.ActivityFileReader;
import com.example.runbuddy.activity.InMemoryRunFileRepository;
import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.Lap;
import com.example.runbuddy.activity.RunDetail;
import com.example.runbuddy.activity.RunFileRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.activity.UnreadableActivityFileException;
import com.example.runbuddy.advisor.AdvisorUnavailableException;
import com.example.runbuddy.shared.Distance;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class RunReviewServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T09:00:00Z");
    private static final Lap LAP = new Lap(1, Duration.ZERO, Distance.ofKilometers(1), Duration.ofMinutes(6), null, null, null, null, null);

    private final RunResultRepository runs = new InMemoryRunResultRepository();
    private final RunFileRepository files = new InMemoryRunFileRepository();
    private final RunReviewRepository reviews = new InMemoryRunReviewRepository();
    private final List<RunReviewRequest> requests = new ArrayList<>();

    private final RunReviewer gemini = reviewer("gemini", "flash");

    @Test
    void reviewsARunAgainstSimilarRunsAcrossTheMonthBoundaryAndSavesIt() {
        // 2 Nov is compared with 30 Oct, which is in the month before.
        runs.saveAll(List.of(run("nov-2", LocalDateTime.of(2026, 11, 2, 6, 0)), run("oct-30", LocalDateTime.of(2026, 10, 30, 6, 0))));

        RunReview review = service(gemini).review("nov-2");

        assertThat(requests.getFirst().similar().runs()).extracting(RunResult::activityId).containsExactly("oct-30");
        assertThat(review).isEqualTo(new RunReview("nov-2", "headline nov-2", "itself", "compared with 1", "gemini", "flash", NOW));
        assertThat(service(gemini).saved("nov-2")).hasValue(review);
    }

    @Test
    void usesTheLapsOnlyWhenTheActivityFileIsAlreadyStored() {
        runs.saveAll(List.of(run("with-file", LocalDateTime.of(2026, 11, 1, 6, 0)), run("without", LocalDateTime.of(2026, 11, 2, 6, 0))));
        files.save("with-file", new byte[] {1});

        service(gemini).review("with-file");
        service(gemini).review("without");

        assertThat(requests).extracting(r -> r.laps().size()).containsExactly(1, 0);
    }

    @Test
    void anUnreadableFileIsReviewedWithoutLaps() {
        runs.saveAll(List.of(run("broken", LocalDateTime.of(2026, 11, 1, 6, 0))));
        files.save("broken", new byte[] {0});

        service(gemini).review("broken");

        assertThat(requests.getFirst().laps()).isEmpty();
    }

    @Test
    void nothingIsSavedWhenTheReviewerFails() {
        runs.saveAll(List.of(run("r1", LocalDateTime.of(2026, 11, 1, 6, 0))));
        RunReviewer failing = new RunReviewer() {
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
                throw new AdvisorUnavailableException("gemini could not answer", null);
            }
        };

        assertThatThrownBy(() -> service(failing).review("r1")).isInstanceOf(AdvisorUnavailableException.class);
        assertThat(reviews.find("r1")).isEmpty();
    }

    @Test
    void aModelThatTakesTooLongTimesOutAndALateAnswerIsNotSaved() throws InterruptedException {
        runs.saveAll(List.of(run("r1", LocalDateTime.of(2026, 11, 1, 6, 0))));
        RunReviewer slow = new RunReviewer() {
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
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new Written("late", "late", "late");
            }
        };

        assertThatThrownBy(() -> service(Duration.ofMillis(100), slow).review("r1"))
                .isInstanceOfSatisfying(AdvisorUnavailableException.class,
                        e -> assertThat(e.reason()).isEqualTo(AdvisorUnavailableException.Reason.TIMED_OUT))
                .hasMessageContaining("did not answer");
        Thread.sleep(700);
        assertThat(reviews.find("r1")).isEmpty();
    }

    @Test
    void anUnknownRunCannotBeReviewed() {
        assertThatThrownBy(() -> service(gemini).review("nope")).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service(gemini).saved("nope")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void prefersARealModelToTheFake() {
        runs.saveAll(List.of(run("r1", LocalDateTime.of(2026, 11, 1, 6, 0))));

        RunReview review = service(reviewer("fake", "rule-based"), gemini).review("r1");

        assertThat(review.reviewer()).isEqualTo("gemini");
    }

    @Test
    void withoutAnyReviewerTheAiIsUnavailable() {
        runs.saveAll(List.of(run("r1", LocalDateTime.of(2026, 11, 1, 6, 0))));

        assertThatThrownBy(() -> service().review("r1")).isInstanceOf(AdvisorUnavailableException.class);
    }

    private RunReviewService service(RunReviewer... reviewers) {
        return service(Duration.ofSeconds(5), reviewers);
    }

    private RunReviewService service(Duration timeout, RunReviewer... reviewers) {
        ActivityFileReader reader = zip -> {
            if (zip[0] == 0) {
                throw new UnreadableActivityFileException("broken", null);
            }
            return new RunDetail(List.of(LAP), List.of());
        };
        return new RunReviewService(runs, files, reader, reviews, List.of(reviewers), Clock.fixed(NOW, ZoneOffset.UTC), timeout);
    }

    private RunReviewer reviewer(String id, String model) {
        return new RunReviewer() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String model() {
                return model;
            }

            @Override
            public Written review(RunReviewRequest request) {
                requests.add(request);
                return new Written("headline " + request.run().activityId(), "itself", "compared with " + request.similar().runs().size());
            }
        };
    }

    private static RunResult run(String id, LocalDateTime start) {
        return new RunResult(id, start, Distance.ofKilometers(10), Duration.ofMinutes(60), 150);
    }
}
