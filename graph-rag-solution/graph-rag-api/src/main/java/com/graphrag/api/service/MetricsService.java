package com.graphrag.api.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 监控指标服务
 */
@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final Counter queryCounter;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Timer queryTimer;
    private final AtomicLong activeQueries = new AtomicLong(0);

    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.queryCounter = Counter.builder("graphrag.queries.total")
                .description("Total number of Graph RAG queries")
                .register(meterRegistry);

        this.successCounter = Counter.builder("graphrag.queries.success")
                .description("Number of successful Graph RAG queries")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("graphrag.queries.error")
                .description("Number of failed Graph RAG queries")
                .register(meterRegistry);

        this.queryTimer = Timer.builder("graphrag.query.duration")
                .description("Graph RAG query execution time")
                .register(meterRegistry);

        // 注册活跃查询数量指标
        meterRegistry.gauge("graphrag.queries.active", activeQueries);
    }

    /**
     * 记录查询开始
     */
    public Timer.Sample startQuery() {
        queryCounter.increment();
        activeQueries.incrementAndGet();
        return Timer.start();
    }

    /**
     * 记录查询成功
     */
    public void recordQuerySuccess(Timer.Sample sample) {
        sample.stop(queryTimer);
        successCounter.increment();
        activeQueries.decrementAndGet();
        logger.debug("查询成功，当前活跃查询数: {}", activeQueries.get());
    }

    /**
     * 记录查询失败
     */
    public void recordQueryError(Timer.Sample sample, String errorType) {
        sample.stop(queryTimer);
        errorCounter.increment();
        activeQueries.decrementAndGet();
        logger.warn("查询失败，错误类型: {}, 当前活跃查询数: {}", errorType, activeQueries.get());
    }

    /**
     * 记录查询延迟
     */
    public void recordQueryLatency(Duration duration) {
        queryTimer.record(duration);
    }

    /**
     * 获取查询统计信息
     */
    public QueryStats getQueryStats() {
        return new QueryStats(
                (long) queryCounter.count(),
                (long) successCounter.count(),
                (long) errorCounter.count(),
                activeQueries.get(),
                queryTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)
        );
    }

    /**
     * 查询统计信息类
     */
    public static class QueryStats {
        private final long totalQueries;
        private final long successfulQueries;
        private final long failedQueries;
        private final long activeQueries;
        private final double averageLatencyMs;

        public QueryStats(long totalQueries, long successfulQueries, long failedQueries, 
                         long activeQueries, double averageLatencyMs) {
            this.totalQueries = totalQueries;
            this.successfulQueries = successfulQueries;
            this.failedQueries = failedQueries;
            this.activeQueries = activeQueries;
            this.averageLatencyMs = averageLatencyMs;
        }

        // Getters
        public long getTotalQueries() { return totalQueries; }
        public long getSuccessfulQueries() { return successfulQueries; }
        public long getFailedQueries() { return failedQueries; }
        public long getActiveQueries() { return activeQueries; }
        public double getAverageLatencyMs() { return averageLatencyMs; }
        
        public double getSuccessRate() {
            return totalQueries > 0 ? (double) successfulQueries / totalQueries : 0.0;
        }
    }
}

