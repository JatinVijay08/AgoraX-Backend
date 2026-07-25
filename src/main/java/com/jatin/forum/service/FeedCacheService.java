package com.jatin.forum.service;

import com.jatin.forum.dto.CachedFeed;
import com.jatin.forum.dto.CachedPost;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Service
public class FeedCacheService {
    public static final String TYPE_NEW = "new";
    public static final String TYPE_HOT = "hot";
    public static final String TYPE_TRENDING = "trending";

    private final RedisTemplate<String, CachedFeed> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private static final Duration FEED_TTL = Duration.ofMinutes(2);
    private static final Duration ACTIVITY_TTL = Duration.ofMinutes(10);
    
    private static final int NEW_THRESHOLD = 1;
    private static final int HOT_THRESHOLD = 10;
    private static final int TRENDING_THRESHOLD = 10;

    public FeedCacheService(RedisTemplate<String,CachedFeed> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private String getFeedKey(String type, int limit) {
        return "feed:" + type + ":size:" + limit;
    }

    private String getActivityKey(String type) {
        return "feed:" + type + ":activity";
    }

    public Optional<CachedFeed> getCachedFeed(String type, int limit) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(getFeedKey(type, limit)));
    }

    public void setCachedFeed(String type, List<CachedPost> cachedPosts, boolean hasMore, Instant cursor, int limit) {
        CachedFeed cachedFeed = new CachedFeed(cachedPosts, hasMore, cursor);
        redisTemplate.opsForValue().set(getFeedKey(type, limit), cachedFeed, FEED_TTL);
    }

    public void evictFeed(String type, int limit) {
        redisTemplate.delete(getFeedKey(type, limit));
    }

    public long incrementActivity(String type) {
        String key = getActivityKey(type);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, ACTIVITY_TTL);
        }
        return count != null ? count : 0;
    }

    public void resetActivity(String type) {
        stringRedisTemplate.delete(getActivityKey(type));
    }

    public boolean shouldEvict(String type, long count) {
        return switch (type) {
            case TYPE_HOT -> count >= HOT_THRESHOLD;
            case TYPE_TRENDING -> count >= TRENDING_THRESHOLD;
            case TYPE_NEW -> count >= NEW_THRESHOLD;
            default -> false;
        };
    }
}
