package com.graphrag.api.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流配置
 */
@Configuration
public class RateLimitConfig {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 创建限流桶
     */
    public Bucket createBucket(String key, long capacity, long refillTokens, Duration refillPeriod) {
        return buckets.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(refillTokens, refillPeriod));
            return Bucket4j.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    /**
     * 查询限流桶（每分钟10次）
     */
    @Bean
    public Bucket queryRateLimitBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * 文档上传限流桶（每小时5次）
     */
    @Bean
    public Bucket uploadRateLimitBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * 获取用户限流桶
     */
    public Bucket getUserBucket(String userId) {
        return createBucket("user:" + userId, 100, 100, Duration.ofHours(1));
    }

    /**
     * 获取IP限流桶
     */
    public Bucket getIpBucket(String ip) {
        return createBucket("ip:" + ip, 50, 50, Duration.ofHours(1));
    }
}

