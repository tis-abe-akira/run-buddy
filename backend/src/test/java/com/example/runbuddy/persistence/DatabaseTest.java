package com.example.runbuddy.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Base for tests against PostgreSQL. Each test acts as a fresh user, so row level security alone
 * keeps tests from seeing each other's data; nothing is cleaned up.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
public abstract class DatabaseTest {

    @Autowired
    protected TestCurrentUser currentUser;

    @BeforeEach
    void actAsNewUser() {
        currentUser.switchToNewUser();
    }
}
