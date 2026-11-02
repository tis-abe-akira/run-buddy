package com.example.runbuddy.race;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port for storing races. Implementations: in memory, jOOQ (persistence/jooq). */
public interface RaceRepository {

    /** Stores the race, replacing the one with the same id. */
    void save(Race race);

    Optional<Race> findById(UUID id);

    /** Every race, earliest first. */
    List<Race> findAll();

    /** @return whether there was a race with this id */
    boolean delete(UUID id);
}
