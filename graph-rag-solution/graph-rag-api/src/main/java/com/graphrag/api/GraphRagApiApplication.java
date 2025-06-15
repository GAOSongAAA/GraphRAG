package com.graphrag.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Graph RAG API 應用程式啟動類別
 * 
 * 此類別負責啟動 Spring Boot 應用程式，並配置基本的掃描包路徑
 * 和配置屬性支援。
 */
@SpringBootApplication(scanBasePackages = "com.graphrag")
@EnableConfigurationProperties
public class GraphRagApiApplication {

    /**
     * 應用程式主入口點
     * 
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        SpringApplication.run(GraphRagApiApplication.class, args);
    }
}
