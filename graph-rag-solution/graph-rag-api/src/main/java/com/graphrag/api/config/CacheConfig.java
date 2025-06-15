package com.graphrag.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * 缓存配置
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 内存缓存管理器（开发环境）
     */
    @Bean
    public CacheManager memoryCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "queryResults",
                "documentEmbeddings",
                "entityEmbeddings",
                "graphTraversalResults"
        ));
        return cacheManager;
    }

    /**
     * Redis 缓存管理器（生产环境）
     */
    // @Bean
    // @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 缓存1小时
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new org.springframework.data.redis.serializer.StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }
}

