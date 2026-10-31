package com.example.runbuddy.activity;

class InMemoryRunResultRepositoryTest implements RunResultRepositoryContract {

    private final RunResultRepository repository = new InMemoryRunResultRepository();

    @Override
    public RunResultRepository repository() {
        return repository;
    }
}
