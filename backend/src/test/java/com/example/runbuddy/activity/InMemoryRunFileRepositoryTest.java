package com.example.runbuddy.activity;

class InMemoryRunFileRepositoryTest implements RunFileRepositoryContract {

    private final RunFileRepository files = new InMemoryRunFileRepository();
    private final RunResultRepository runs = new InMemoryRunResultRepository();

    @Override
    public RunFileRepository files() {
        return files;
    }

    @Override
    public RunResultRepository runs() {
        return runs;
    }
}
