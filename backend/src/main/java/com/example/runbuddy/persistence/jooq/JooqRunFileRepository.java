package com.example.runbuddy.persistence.jooq;

import static com.example.runbuddy.persistence.jooq.generated.Tables.RUN_FIT_FILE;

import com.example.runbuddy.activity.RunFileRepository;
import java.util.Optional;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

/** Activity files in PostgreSQL. Row level security keeps each user to their own rows (ADR-0007). */
@Transactional
public class JooqRunFileRepository implements RunFileRepository {

    private final DSLContext dsl;

    public JooqRunFileRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(String activityId, byte[] fitZip) {
        dsl.insertInto(RUN_FIT_FILE)
                .set(RUN_FIT_FILE.ACTIVITY_ID, activityId)
                .set(RUN_FIT_FILE.FIT_ZIP, fitZip)
                .onConflict(RUN_FIT_FILE.USER_ID, RUN_FIT_FILE.ACTIVITY_ID)
                .doNothing()
                .execute();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> find(String activityId) {
        return dsl.select(RUN_FIT_FILE.FIT_ZIP)
                .from(RUN_FIT_FILE)
                .where(RUN_FIT_FILE.ACTIVITY_ID.eq(activityId))
                .fetchOptional(RUN_FIT_FILE.FIT_ZIP);
    }
}
