package com.example.runbuddy.persistence.jooq;

import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "runbuddy.persistence", havingValue = "jooq")
public class JooqPersistenceConfig {

    @Bean
    JooqMonthlyPlanRepository monthlyPlanRepository(DSLContext dsl) {
        return new JooqMonthlyPlanRepository(dsl);
    }

    @Bean
    JooqRunResultRepository runResultRepository(DSLContext dsl) {
        return new JooqRunResultRepository(dsl);
    }
}
