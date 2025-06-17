package com.graphrag.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Transaction Manager Configuration
 * Resolves conflicts between multiple transaction managers
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * Set Neo4j transaction manager as the primary transaction manager
     */
    @Bean
    @Primary
    @Qualifier("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(
            org.neo4j.driver.Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}