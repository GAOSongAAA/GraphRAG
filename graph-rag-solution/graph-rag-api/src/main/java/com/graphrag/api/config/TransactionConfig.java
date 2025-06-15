package com.graphrag.api.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 事務管理器配置
 * 解決多個事務管理器衝突問題
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * 設置 Neo4j 事務管理器為主要事務管理器
     */
    @Bean
    @Primary
    @Qualifier("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(
            org.neo4j.driver.Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
} 