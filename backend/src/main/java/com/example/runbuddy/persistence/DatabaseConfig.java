package com.example.runbuddy.persistence;

import com.example.runbuddy.shared.CurrentUser;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Shared by every database-backed persistence mode. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'${runbuddy.persistence}' != 'memory'")
public class DatabaseConfig {

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource, CurrentUser currentUser) {
        return new RlsTransactionManager(dataSource, currentUser);
    }
}
