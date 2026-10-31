package com.example.runbuddy.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * A real PostgreSQL in a container, shared by all database tests in the run.
 *
 * <p>The application connects as runbuddy_app (created by db/init), exactly as in production, so
 * row level security is in force. The container's own user is a superuser and would bypass it;
 * it is only used by Flyway to migrate.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("db/init"), "/docker-entrypoint-initdb.d/");

    static {
        POSTGRES.start();
    }

    @Bean
    DynamicPropertyRegistrar postgresProperties() {
        return registry -> {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", () -> "runbuddy_app");
            registry.add("spring.datasource.password", () -> "runbuddy_app");
            registry.add("spring.flyway.user", POSTGRES::getUsername);
            registry.add("spring.flyway.password", POSTGRES::getPassword);
        };
    }

    @Bean
    @Primary
    TestCurrentUser testCurrentUser() {
        return new TestCurrentUser();
    }
}
