package org.example.marketplace.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Redis cache manager with appropriate TTL settings
     * - Listings cache: 30 minutes
     * - User profiles: 1 hour
     * - Search results: 15 minutes
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Default cache configuration with TTL of 10 minutes
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));

        // Configure specific cache TTLs
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withCacheConfiguration("listings",
                        defaultCacheConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("users",
                        defaultCacheConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("searchResults",
                        defaultCacheConfig.entryTtl(Duration.ofMinutes(15)))
                .build();
    }
}
