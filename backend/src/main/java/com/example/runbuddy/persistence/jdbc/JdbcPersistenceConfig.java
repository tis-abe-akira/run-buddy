package com.example.runbuddy.persistence.jdbc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;

/**
 * Spring Data JDBC adapters. They use {@link JdbcAggregateTemplate} directly rather than
 * repository interfaces, because the ports are already defined by the domain.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "runbuddy.persistence", havingValue = "jdbc")
public class JdbcPersistenceConfig {

    @Bean
    JdbcMonthlyPlanRepository monthlyPlanRepository(JdbcAggregateTemplate template) {
        return new JdbcMonthlyPlanRepository(template);
    }

    @Bean
    JdbcRunResultRepository runResultRepository(JdbcAggregateTemplate template) {
        return new JdbcRunResultRepository(template);
    }
}
