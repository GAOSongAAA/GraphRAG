package com.graphrag.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Asynchronous Configuration
 * Asynchronous Configuration for thread pool management
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Graph RAG Query Thread Pool
     * Graph RAG query thread pool executor
     * Core: 5, Max: 20, Queue: 100 - Handles graph RAG query operations
     */
    @Bean("graphRagExecutor")
    public Executor graphRagExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);           // Core pool size for concurrent processing
        executor.setMaxPoolSize(20);           // Maximum pool size for peak load
        executor.setQueueCapacity(100);        // Queue capacity for pending tasks
        executor.setThreadNamePrefix("GraphRAG-");  // Thread name prefix for identification
        executor.setKeepAliveSeconds(60);      // Keep alive time for idle threads
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Document Processing Thread Pool
     * Document processing thread pool executor
     * Core: 3, Max: 10, Queue: 50 - Handles document analysis and processing
     */
    @Bean("documentProcessorExecutor")
    public Executor documentProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);           // Core pool size for document processing
        executor.setMaxPoolSize(10);           // Maximum pool size for document workload
        executor.setQueueCapacity(50);         // Queue capacity for document tasks
        executor.setThreadNamePrefix("DocProcessor-");  // Thread name prefix for identification
        executor.setKeepAliveSeconds(60);      // Keep alive time for idle threads
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Embedding Processing Thread Pool
     * Embedding processing thread pool executor
     * Core: 2, Max: 8, Queue: 30 - Handles vector embedding operations
     */
    @Bean("embeddingExecutor")
    public Executor embeddingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);           // Core pool size for embedding operations
        executor.setMaxPoolSize(8);            // Maximum pool size for embedding workload
        executor.setQueueCapacity(30);         // Queue capacity for embedding tasks
        executor.setThreadNamePrefix("Embedding-");  // Thread name prefix for identification
        executor.setKeepAliveSeconds(60);      // Keep alive time for idle threads
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
