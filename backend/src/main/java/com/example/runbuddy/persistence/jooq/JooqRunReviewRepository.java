package com.example.runbuddy.persistence.jooq;

import static com.example.runbuddy.persistence.jooq.generated.Tables.RUN_REVIEW;

import com.example.runbuddy.persistence.jooq.generated.tables.records.RunReviewRecord;
import com.example.runbuddy.review.RunReview;
import com.example.runbuddy.review.RunReviewRepository;
import java.time.ZoneOffset;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

/** Run reviews in PostgreSQL. Row level security keeps each user to their own rows (ADR-0007). */
@Transactional
public class JooqRunReviewRepository implements RunReviewRepository {

    private final DSLContext dsl;

    public JooqRunReviewRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(RunReview review) {
        dsl.insertInto(RUN_REVIEW)
                .set(RUN_REVIEW.ACTIVITY_ID, review.activityId())
                .set(RUN_REVIEW.HEADLINE, review.headline())
                .set(RUN_REVIEW.RUN_ITSELF, review.runItself())
                .set(RUN_REVIEW.COMPARISON, review.comparison())
                .set(RUN_REVIEW.REVIEWER, review.reviewer())
                .set(RUN_REVIEW.MODEL, review.model())
                .set(RUN_REVIEW.REVIEWED_AT, review.reviewedAt().atOffset(ZoneOffset.UTC))
                .onConflict(RUN_REVIEW.USER_ID, RUN_REVIEW.ACTIVITY_ID)
                .doUpdate()
                .set(RUN_REVIEW.HEADLINE, review.headline())
                .set(RUN_REVIEW.RUN_ITSELF, review.runItself())
                .set(RUN_REVIEW.COMPARISON, review.comparison())
                .set(RUN_REVIEW.REVIEWER, review.reviewer())
                .set(RUN_REVIEW.MODEL, review.model())
                .set(RUN_REVIEW.REVIEWED_AT, review.reviewedAt().atOffset(ZoneOffset.UTC))
                .execute();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RunReview> find(String activityId) {
        return dsl.selectFrom(RUN_REVIEW)
                .where(RUN_REVIEW.ACTIVITY_ID.eq(activityId))
                .fetchOptional()
                .map(JooqRunReviewRepository::toReview);
    }

    private static RunReview toReview(RunReviewRecord row) {
        return new RunReview(row.getActivityId(), row.getHeadline(), row.getRunItself(), row.getComparison(),
                row.getReviewer(), row.getModel(), row.getReviewedAt().toInstant());
    }
}
