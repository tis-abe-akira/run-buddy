package com.example.runbuddy.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.runbuddy.activity.RunFileRepository;
import com.example.runbuddy.review.RunReview;
import com.example.runbuddy.review.RunReviewRepository;
import com.example.runbuddy.activity.RunResult;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.MonthlyPlan;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.PlannedWorkout;
import com.example.runbuddy.plan.WorkoutType;
import com.example.runbuddy.race.Race;
import com.example.runbuddy.race.RaceRepository;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Users never see or overwrite each other's data, through any adapter. */
public interface RowLevelSecurityContract {

    YearMonth NOVEMBER = YearMonth.of(2026, 11);

    TestCurrentUser currentUser();

    MonthlyPlanRepository plans();

    RunResultRepository runs();

    RaceRepository races();

    RunFileRepository files();

    RunReviewRepository reviews();

    @Test
    default void anotherUserSeesNothing() {
        String alice = currentUser().switchToNewUser();
        plans().save(plan(100));
        runs().saveAll(List.of(run("a")));

        currentUser().switchToNewUser();

        assertThat(plans().findByMonth(NOVEMBER)).isEmpty();
        assertThat(runs().findByMonth(NOVEMBER)).isEmpty();
        assertThat(runs().latestStart()).isEmpty();

        currentUser().switchTo(alice);
        assertThat(plans().findByMonth(NOVEMBER)).contains(plan(100));
    }

    @Test
    default void sameMonthAndActivityIdDoNotCollideAcrossUsers() {
        String alice = currentUser().switchToNewUser();
        plans().save(plan(100));
        runs().saveAll(List.of(run("a")));

        currentUser().switchToNewUser();
        plans().save(plan(200));
        assertThat(runs().saveAll(List.of(run("a")))).hasSize(1);
        currentUser().switchTo(alice);
        assertThat(plans().findByMonth(NOVEMBER)).contains(plan(100));
    }

    @Test
    default void anotherUserSeesNoRaces() {
        String alice = currentUser().switchToNewUser();
        Race race = race("Alice's marathon");
        races().save(race);

        currentUser().switchToNewUser();

        assertThat(races().findAll()).isEmpty();
        assertThat(races().findById(race.id())).isEmpty();

        currentUser().switchTo(alice);
        assertThat(races().findAll()).containsExactly(race);
    }

    @Test
    default void anotherUserCanNeitherOverwriteNorDeleteARace() {
        String alice = currentUser().switchToNewUser();
        Race race = race("Alice's marathon");
        races().save(race);

        currentUser().switchToNewUser();
        // Alice's race is invisible, so there is nothing to delete...
        assertThat(races().delete(race.id())).isFalse();
        // ...and saving the same id creates this user's own race instead of changing hers.
        races().save(new Race(race.id(), "Mallory's race", race.date(), race.distance(), null));
        assertThat(races().findAll()).extracting(Race::name).containsExactly("Mallory's race");

        currentUser().switchTo(alice);
        assertThat(races().findAll()).containsExactly(race);
    }

    @Test
    default void anotherUserSeesNoActivityFile() {
        String alice = currentUser().switchToNewUser();
        runs().saveAll(List.of(run("a")));
        files().save("a", new byte[] {1, 2, 3});

        currentUser().switchToNewUser();
        assertThat(files().find("a")).isEmpty();

        currentUser().switchTo(alice);
        assertThat(files().find("a")).hasValueSatisfying(zip -> assertThat(zip).containsExactly(1, 2, 3));
    }

    @Test
    default void anotherUserSeesNoRunReview() {
        String alice = currentUser().switchToNewUser();
        runs().saveAll(List.of(run("a")));
        RunReview review = new RunReview("a", "よく走れました", "A", "C", "gemini", "flash", java.time.Instant.parse("2026-11-02T07:00:00Z"));
        reviews().save(review);

        currentUser().switchToNewUser();
        assertThat(reviews().find("a")).isEmpty();

        currentUser().switchTo(alice);
        assertThat(reviews().find("a")).hasValue(review);
    }

    private static Race race(String name) {
        return Race.plan(name, LocalDate.of(2026, 11, 22), Distance.ofMeters(42_195), Duration.ofHours(5));
    }

    private static MonthlyPlan plan(double targetKm) {
        return new MonthlyPlan(NOVEMBER, Distance.ofKilometers(targetKm), List.of(
                new PlannedWorkout(NOVEMBER.atDay(1), WorkoutType.EASY, Distance.ofKilometers(5), "")));
    }

    private static RunResult run(String id) {
        return new RunResult(id, LocalDateTime.of(2026, 11, 2, 6, 0), Distance.ofKilometers(5), Duration.ofMinutes(30), null);
    }
}
