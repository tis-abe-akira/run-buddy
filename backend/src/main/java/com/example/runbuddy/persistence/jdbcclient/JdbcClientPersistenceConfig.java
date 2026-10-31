package com.example.runbuddy.persistence.jdbcclient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Plain SQL through Spring's JdbcClient. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "runbuddy.persistence", havingValue = "jdbcclient")
public class JdbcClientPersistenceConfig {

    @Bean
    JdbcClientMonthlyPlanRepository monthlyPlanRepository(JdbcClient jdbc, NamedParameterJdbcTemplate batch) {
        return new JdbcClientMonthlyPlanRepository(jdbc, batch);
    }

    @Bean
    JdbcClientRunResultRepository runResultRepository(JdbcClient jdbc, NamedParameterJdbcTemplate batch) {
        return new JdbcClientRunResultRepository(jdbc, batch);
    }
}
