package com.example.runbuddy.plan;

class InMemoryMonthlyPlanRepositoryTest implements MonthlyPlanRepositoryContract {

    private final MonthlyPlanRepository repository = new InMemoryMonthlyPlanRepository();

    @Override
    public MonthlyPlanRepository repository() {
        return repository;
    }
}
