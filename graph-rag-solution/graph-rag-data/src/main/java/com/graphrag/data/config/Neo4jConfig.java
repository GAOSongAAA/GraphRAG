package com.graphrag.data.config;

import com.graphrag.common.config.GraphRagProperties;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * Neo4j 數據庫配置
 */
@Configuration
@EnableNeo4jRepositories(basePackages = "com.graphrag.data.repository")
@Profile("!test") // 測試環境下不啟用
public class Neo4jConfig extends AbstractNeo4jConfig {

    private static final Logger logger = LoggerFactory.getLogger(Neo4jConfig.class);

    @Autowired
    private GraphRagProperties properties;

    @Bean
    @Override
    public Driver driver() {
        try {
            logger.info("正在連接 Neo4j 數據庫: {}", properties.getNeo4j().getUri());
            Driver driver = GraphDatabase.driver(
                properties.getNeo4j().getUri(),
                AuthTokens.basic(
                    properties.getNeo4j().getUsername(),
                    properties.getNeo4j().getPassword()
                )
            );
            
            // 測試連接
            driver.verifyConnectivity();
            logger.info("Neo4j 數據庫連接成功");
            return driver;
        } catch (Exception e) {
            logger.error("Neo4j 數據庫連接失敗: {}", e.getMessage());
            throw new RuntimeException("無法連接到 Neo4j 數據庫", e);
        }
    }


}
