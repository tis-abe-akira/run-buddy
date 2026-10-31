package com.example.runbuddy.advisor.fake;

import com.example.runbuddy.advisor.AdvisorTools;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Offers the rule-based fake advisor. Turn off with runbuddy.ai.fake.enabled=false in production. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "runbuddy.ai.fake.enabled", havingValue = "true")
public class FakeAdvisorConfig {

    @Bean
    FakeTrainingAdvisor fakeAdvisor(AdvisorTools tools) {
        return new FakeTrainingAdvisor(tools);
    }
}
