package com.graphrag.data.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 測試環境配置
 * 在測試環境下禁用 Neo4j 相關配置
 */
@Configuration
@Profile("test")
public class TestConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);

    public TestConfig() {
        logger.info("測試環境配置已啟用，將使用模擬服務");
    }
} 