package com.jatin.forum.service;

import com.jatin.forum.dto.CachedFeed;
import com.jatin.forum.dto.CachedPost;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class FeedCacheService {
    private final RedisTemplate<String, CachedFeed> redisTemplate;

    FeedCacheService(RedisTemplate<String,CachedFeed> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<CachedFeed> getCachedNewPosts(int limit){
        return Optional.ofNullable(redisTemplate.opsForValue().get(getNewKey(limit)));
    }

    public Optional<CachedFeed> getCachedHotPosts(int limit){
        return Optional.ofNullable(redisTemplate.opsForValue().get(getHotKey(limit)));
    }

    public Optional<CachedFeed> getCachedTrendingPosts(int limit){
        return Optional.ofNullable(redisTemplate.opsForValue().get(getTrendingKey(limit)));
    }


    private String getTrendingKey(int limit){
        return "feed"+"trending"+"size"+limit;
    }

    private String getHotKey(int limit){
        return "feed:"+"hot:"+"size:"+limit;
    }

    private String getNewKey(int limit){
        return "feed:"+"new:"+"size:"+limit;
    }

    public void setCachedNewPosts(List<CachedPost> cachedPosts, boolean hasMore, Instant cursor,int limit){
        CachedFeed cachedFeed = new CachedFeed(cachedPosts, hasMore, cursor);
        redisTemplate.opsForValue().set(getNewKey(limit),cachedFeed);
    }

    public void  setCachedHotPosts(List<CachedPost> cachedPosts, boolean hasMore, Instant cursor,int limit){
        CachedFeed cachedFeed = new CachedFeed(cachedPosts, hasMore, cursor);
        redisTemplate.opsForValue().set(getHotKey(limit),cachedFeed);
    }

    public void setCachedTrendingPosts(List<CachedPost> cachedPosts, boolean hasMore, Instant cursor,int limit){
        CachedFeed cachedFeed = new CachedFeed(cachedPosts, hasMore, cursor);
        redisTemplate.opsForValue().set(getTrendingKey(limit),cachedFeed);
    }

}
