package com.example.runbuddy.persistence.jooq;

import com.example.runbuddy.activity.RunFileRepository;
import com.example.runbuddy.activity.RunFileRepositoryContract;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.activity.RunResultRepositoryContract;
import com.example.runbuddy.persistence.ApiOverDatabaseContract;
import com.example.runbuddy.persistence.DatabaseTest;
import com.example.runbuddy.persistence.RowLevelSecurityContract;
import com.example.runbuddy.persistence.TestCurrentUser;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlanRepositoryContract;
import com.example.runbuddy.race.RaceRepository;
import com.example.runbuddy.race.RaceRepositoryContract;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties = "runbuddy.persistence=jooq")
class JooqPersistenceTest extends DatabaseTest {

    @Autowired
    MonthlyPlanRepository plans;

    @Autowired
    RunResultRepository runs;

    @Autowired
    RaceRepository races;

    @Autowired
    RunFileRepository files;

    @Autowired
    MockMvc mockMvc;

    @Nested
    class Plans implements MonthlyPlanRepositoryContract {
        @Override
        public MonthlyPlanRepository repository() {
            return plans;
        }
    }

    @Nested
    class Runs implements RunResultRepositoryContract {
        @Override
        public RunResultRepository repository() {
            return runs;
        }
    }

    @Nested
    class Files implements RunFileRepositoryContract {
        @Override
        public RunFileRepository files() {
            return files;
        }

        @Override
        public RunResultRepository runs() {
            return runs;
        }
    }

    @Nested
    class Races implements RaceRepositoryContract {
        @Override
        public RaceRepository repository() {
            return races;
        }
    }

    @Nested
    class Isolation implements RowLevelSecurityContract {
        @Override
        public TestCurrentUser currentUser() {
            return currentUser;
        }

        @Override
        public MonthlyPlanRepository plans() {
            return plans;
        }

        @Override
        public RunResultRepository runs() {
            return runs;
        }

        @Override
        public RaceRepository races() {
            return races;
        }

        @Override
        public RunFileRepository files() {
            return files;
        }
    }

    @Nested
    class Api implements ApiOverDatabaseContract {
        @Override
        public MockMvc mockMvc() {
            return mockMvc;
        }
    }
}
