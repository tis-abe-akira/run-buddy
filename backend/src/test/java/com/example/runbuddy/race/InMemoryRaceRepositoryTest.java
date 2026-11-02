package com.example.runbuddy.race;

class InMemoryRaceRepositoryTest implements RaceRepositoryContract {

    private final RaceRepository repository = new InMemoryRaceRepository();

    @Override
    public RaceRepository repository() {
        return repository;
    }
}
