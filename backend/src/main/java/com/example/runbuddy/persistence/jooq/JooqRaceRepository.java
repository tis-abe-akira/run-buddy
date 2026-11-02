package com.example.runbuddy.persistence.jooq;

import static com.example.runbuddy.persistence.jooq.generated.Tables.RACE;

import com.example.runbuddy.persistence.jooq.generated.tables.records.RaceRecord;
import com.example.runbuddy.race.Race;
import com.example.runbuddy.race.RaceRepository;
import com.example.runbuddy.shared.Distance;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.transaction.annotation.Transactional;

/** Races in PostgreSQL. Row level security keeps each user to their own rows (ADR-0007). */
@Transactional
public class JooqRaceRepository implements RaceRepository {

    private final DSLContext dsl;

    public JooqRaceRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(Race race) {
        Long targetSeconds = race.targetTime() == null ? null : race.targetTime().toSeconds();
        dsl.insertInto(RACE)
                .set(RACE.ID, race.id())
                .set(RACE.NAME, race.name())
                .set(RACE.RACE_DATE, race.date())
                .set(RACE.DISTANCE_METERS, race.distance().meters())
                .set(RACE.TARGET_SECONDS, targetSeconds)
                .onConflict(RACE.USER_ID, RACE.ID)
                .doUpdate()
                .set(RACE.NAME, race.name())
                .set(RACE.RACE_DATE, race.date())
                .set(RACE.DISTANCE_METERS, race.distance().meters())
                .set(RACE.TARGET_SECONDS, targetSeconds)
                .execute();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Race> findById(UUID id) {
        return dsl.selectFrom(RACE).where(RACE.ID.eq(id)).fetchOptional(JooqRaceRepository::toRace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Race> findAll() {
        return dsl.selectFrom(RACE).orderBy(RACE.RACE_DATE, RACE.NAME).fetch(JooqRaceRepository::toRace);
    }

    @Override
    public boolean delete(UUID id) {
        return dsl.deleteFrom(RACE).where(RACE.ID.eq(id)).execute() > 0;
    }

    private static Race toRace(RaceRecord row) {
        return new Race(
                row.getId(),
                row.getName(),
                row.getRaceDate(),
                Distance.ofMeters(row.getDistanceMeters()),
                row.getTargetSeconds() == null ? null : Duration.ofSeconds(row.getTargetSeconds()));
    }
}
