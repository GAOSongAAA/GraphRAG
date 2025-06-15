package com.graphrag.performance;

import com.graphrag.core.model.GraphRagRequest;
import com.graphrag.core.model.GraphRagResponse;
import com.graphrag.core.service.GraphRagRetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能测试
 */
@SpringBootTest
@ActiveProfiles("test")
class PerformanceTest {

    @Autowired
    private GraphRagRetrievalService retrievalService;

    @Test
    void testQueryPerformance_SingleThread() {
        String[] testQueries = {
                "什么是人工智能？",
                "机器学习的基本原理是什么？",
                "深度学习和机器学习的区别？",
                "神经网络是如何工作的？",
                "自然语言处理的应用场景有哪些？"
        };

        long totalTime = 0;
        int successCount = 0;

        for (String query : testQueries) {
            try {
                long startTime = System.currentTimeMillis();
                
                GraphRagRequest request = new GraphRagRequest(query);
                GraphRagResponse response = retrievalService.retrieve(request);
                
                long endTime = System.currentTimeMillis();
                long queryTime = endTime - startTime;
                totalTime += queryTime;
                
                assertNotNull(response);
                assertNotNull(response.getAnswer());
                successCount++;
                
                System.out.printf("查询: %s, 耗时: %d ms%n", query, queryTime);
                
            } catch (Exception e) {
                System.err.printf("查询失败: %s, 错误: %s%n", query, e.getMessage());
            }
        }

        double averageTime = (double) totalTime / successCount;
        System.out.printf("单线程性能测试完成 - 成功: %d/%d, 平均耗时: %.2f ms%n", 
                successCount, testQueries.length, averageTime);
        
        // 性能断言
        assertTrue(averageTime < 5000, "平均查询时间应小于5秒");
        assertEquals(testQueries.length, successCount, "所有查询都应该成功");
    }

    @Test
    void testQueryPerformance_MultiThread() throws InterruptedException {
        String[] testQueries = {
                "什么是人工智能？",
                "机器学习的基本原理是什么？",
                "深度学习和机器学习的区别？",
                "神经网络是如何工作的？",
                "自然语言处理的应用场景有哪些？"
        };

        int threadCount = 5;
        int queriesPerThread = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long threadTime = 0;
                for (int j = 0; j < queriesPerThread; j++) {
                    try {
                        String query = testQueries[j % testQueries.length];
                        long queryStart = System.currentTimeMillis();
                        
                        GraphRagRequest request = new GraphRagRequest(query);
                        GraphRagResponse response = retrievalService.retrieve(request);
                        
                        long queryEnd = System.currentTimeMillis();
                        threadTime += (queryEnd - queryStart);
                        
                        assertNotNull(response);
                        
                    } catch (Exception e) {
                        System.err.printf("线程 %d 查询失败: %s%n", threadIndex, e.getMessage());
                    }
                }
                return threadTime;
            }, executor);
            
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        long totalQueryTime = futures.stream()
                .mapToLong(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        int totalQueries = threadCount * queriesPerThread;
        double averageQueryTime = (double) totalQueryTime / totalQueries;
        double throughput = (double) totalQueries / (totalTime / 1000.0);

        System.out.printf("多线程性能测试完成 - 总查询数: %d, 总耗时: %d ms, 平均查询时间: %.2f ms, 吞吐量: %.2f queries/sec%n",
                totalQueries, totalTime, averageQueryTime, throughput);

        // 性能断言
        assertTrue(averageQueryTime < 10000, "多线程环境下平均查询时间应小于10秒");
        assertTrue(throughput > 0.1, "吞吐量应大于0.1 queries/sec");
    }

    @Test
    void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        // 记录初始内存使用
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 执行大量查询
        for (int i = 0; i < 50; i++) {
            try {
                GraphRagRequest request = new GraphRagRequest("测试查询 " + i);
                GraphRagResponse response = retrievalService.retrieve(request);
                assertNotNull(response);
            } catch (Exception e) {
                // 忽略错误，专注于内存测试
            }
        }
        
        // 强制垃圾回收
        System.gc();
        Thread.yield();
        
        // 记录最终内存使用
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.printf("内存使用测试 - 初始: %d MB, 最终: %d MB, 增长: %d MB%n",
                initialMemory / 1024 / 1024, finalMemory / 1024 / 1024, memoryIncrease / 1024 / 1024);
        
        // 内存使用断言（增长不应超过100MB）
        assertTrue(memoryIncrease < 100 * 1024 * 1024, "内存增长应小于100MB");
    }

    @Test
    void testConcurrentQueries() throws InterruptedException {
        int concurrentUsers = 10;
        int queriesPerUser = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                boolean allSuccess = true;
                for (int j = 0; j < queriesPerUser; j++) {
                    try {
                        String query = String.format("用户 %d 的查询 %d", userId, j);
                        GraphRagRequest request = new GraphRagRequest(query);
                        GraphRagResponse response = retrievalService.retrieve(request);
                        
                        if (response == null || response.getAnswer() == null) {
                            allSuccess = false;
                        }
                        
                    } catch (Exception e) {
                        System.err.printf("用户 %d 查询 %d 失败: %s%n", userId, j, e.getMessage());
                        allSuccess = false;
                    }
                }
                return allSuccess;
            }, executor);
            
            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        long successfulUsers = futures.stream()
                .mapToLong(future -> {
                    try {
                        return future.get() ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.printf("并发测试完成 - 并发用户: %d, 成功用户: %d, 总耗时: %d ms%n",
                concurrentUsers, successfulUsers, totalTime);

        // 并发测试断言
        assertTrue(successfulUsers >= concurrentUsers * 0.8, "至少80%的用户应该成功完成所有查询");
        assertTrue(totalTime < 60000, "并发测试总时间应小于60秒");
    }
}

