package com.jatin.forum.config;

import com.jatin.forum.dto.CachedFeed;
import com.jatin.forum.dto.CachedPost;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.util.List;


@Configuration
@EnableCaching
public class CacheConfig {


    @Bean
    public RedisTemplate<String, CachedFeed> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, CachedFeed> redisTemplate = new RedisTemplate<>();
        redisTemplate.setValueSerializer(new JacksonJsonRedisSerializer<>(CachedFeed.class));
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(factory);
        return redisTemplate;
    }


}
