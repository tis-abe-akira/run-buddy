package com.example.runbuddy.race;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory storage for the "memory" profile and for tests. Data is lost on restart. */
public class InMemoryRaceRepository implements RaceRepository {

    private final Map<UUID, Race> racesById = new ConcurrentHashMap<>();

    @Override
    public void save(Race race) {
        racesById.put(race.id(), race);
    }

    @Override
    public Optional<Race> findById(UUID id) {
        return Optional.ofNullable(racesById.get(id));
    }

    @Override
    public List<Race> findAll() {
        return racesById.values().stream()
                .sorted(Comparator.comparing(Race::date).thenComparing(Race::name))
                .toList();
    }

    @Override
    public boolean delete(UUID id) {
        return racesById.remove(id) != null;
    }
}
