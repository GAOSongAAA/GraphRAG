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
 * Rate Limiting Configuration
 */
@Configuration
public class RateLimitConfig {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Create a rate limit bucket
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
     * Query rate limit bucket (10 requests per minute)
     */
    @Bean
    public Bucket queryRateLimitBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Document upload rate limit bucket (5 uploads per hour)
     */
    @Bean
    public Bucket uploadRateLimitBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Get user rate limit bucket
     */
    public Bucket getUserBucket(String userId) {
        return createBucket("user:" + userId, 100, 100, Duration.ofHours(1));
    }

    /**
     * Get IP rate limit bucket
     */
    public Bucket getIpBucket(String ip) {
        return createBucket("ip:" + ip, 50, 50, Duration.ofHours(1));
    }
}
