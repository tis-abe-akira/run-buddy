package com.example.runbuddy.persistence.jdbcclient;

import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.activity.RunResultRepositoryContract;
import com.example.runbuddy.persistence.ApiOverDatabaseContract;
import com.example.runbuddy.persistence.DatabaseTest;
import com.example.runbuddy.persistence.RowLevelSecurityContract;
import com.example.runbuddy.persistence.TestCurrentUser;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlanRepositoryContract;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties = "runbuddy.persistence=jdbcclient")
class JdbcClientPersistenceTest extends DatabaseTest {

    @Autowired
    MonthlyPlanRepository plans;

    @Autowired
    RunResultRepository runs;

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
    }

    @Nested
    class Api implements ApiOverDatabaseContract {
        @Override
        public MockMvc mockMvc() {
            return mockMvc;
        }
    }
}
