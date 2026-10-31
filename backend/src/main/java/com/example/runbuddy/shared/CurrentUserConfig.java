package com.example.runbuddy.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CurrentUserConfig {

    /** There is no sign-in yet, so everyone is the same configured user. */
    @Bean
    CurrentUser currentUser(@Value("${runbuddy.default-user-id}") String userId) {
        return () -> userId;
    }
}
