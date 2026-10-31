package com.example.runbuddy.persistence.memory;

import com.example.runbuddy.activity.InMemoryRunResultRepository;
import com.example.runbuddy.activity.RunResultRepository;
import com.example.runbuddy.plan.InMemoryMonthlyPlanRepository;
import com.example.runbuddy.plan.MonthlyPlanRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** No database: data lives in memory and is lost on restart. Use with the "memory" profile. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "runbuddy.persistence", havingValue = "memory")
public class MemoryPersistenceConfig {

    @Bean
    MonthlyPlanRepository monthlyPlanRepository() {
        return new InMemoryMonthlyPlanRepository();
    }

    @Bean
    RunResultRepository runResultRepository() {
        return new InMemoryRunResultRepository();
    }
}
