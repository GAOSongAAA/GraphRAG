package com.graphrag.data.config;

import com.graphrag.data.service.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器
 */
@Component
@Profile("!test") // 测试环境下不执行
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private GraphService graphService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("开始初始化 Neo4j 数据库...");
        
        try {
            // 初始化数据库约束和索引
            graphService.initializeDatabase();
            
            // 打印数据库统计信息
            var stats = graphService.getDatabaseStats();
            logger.info("数据库统计信息: {}", stats);
            
            logger.info("Neo4j 数据库初始化完成");
        } catch (Exception e) {
            logger.error("数据库初始化失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }
}

