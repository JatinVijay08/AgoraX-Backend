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
    private final RedisTemplate<String, CachedFeed> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private static final Duration NEW_FEED_TTL =  Duration.ofMinutes(2);
    private static final Duration HOT_FEED_TTL =   Duration.ofMinutes(2);
    private static final Duration TRENDING_FEED_TTL = Duration.ofMinutes(2);
    private static final int NEW_THRESHOLD = 1;
    private static final int HOT_THRESHOLD = 10;
    private static final int TRENDING_THRESHOLD = 10;

    public FeedCacheService(RedisTemplate<String,CachedFeed> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
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
        return "feed:"+"trending:"+"size:"+limit;
    }

    private String getHotKey(int limit){
        return "feed:"+"hot:"+"size:"+limit;
    }

    private String getNewKey(int limit){
        return "feed:"+"new:"+"size:"+limit;
    }

    private String getNewActivityKey(){
        return "feed:new:activity";
    }

    private String getHotActivityKey(){
        return "feed:hot:activity";
    }

    private String getTrendingActivityKey(){
        return "feed:trending:activity";
    }

    public void setCachedNewPosts(List<CachedPost> cachedPosts, boolean hasMore, Instant cursor,int limit){
        CachedFeed cachedFeed = new CachedFeed(cachedPosts, hasMore, cursor);
        redisTemplate.opsForValue().set(getNewKey(limit),cachedFeed,NEW_FEED_TTL);
    }

    public void  setCachedHotPosts(List<CachedPost> cachedPosts, boolean hasMore, Instant cursor,int limit){
        CachedFeed cachedFeed = new CachedFeed(cachedPosts, hasMore, cursor);
        redisTemplate.opsForValue().set(getHotKey(limit),cachedFeed,HOT_FEED_TTL);
    }

    public void setCachedTrendingPosts(List<CachedPost> cachedPosts, boolean hasMore, Instant cursor,int limit){
        CachedFeed cachedFeed = new CachedFeed(cachedPosts, hasMore, cursor);
        redisTemplate.opsForValue().set(getTrendingKey(limit),cachedFeed,TRENDING_FEED_TTL);
    }

    public void evictNewFeed(int limit){
        redisTemplate.delete(getNewKey(limit));
    }

    public void evictHotFeed(int limit){
        redisTemplate.delete(getHotKey(limit));
    }

    public void evictTrendingFeed(int limit){
        redisTemplate.delete(getTrendingKey(limit));
    }

    public long incrementNewActivity(){
        return stringRedisTemplate.opsForValue().increment(getNewActivityKey());
    }

    public long incrementTrendingActivity(){
        return  stringRedisTemplate.opsForValue().increment(getTrendingActivityKey());
    }

    public long incrementHotActivity(){
        return stringRedisTemplate.opsForValue().increment(getHotActivityKey());
    }

    public void resetNewActivity(){
        stringRedisTemplate.delete(getNewActivityKey());
    }

    public void resetHotActivity(){
        stringRedisTemplate.delete(getHotActivityKey());
    }

    public void resetTrendingActivity(){
        stringRedisTemplate.delete(getTrendingActivityKey());
    }

    public boolean shouldEvictHot(long count){
        return count>= HOT_THRESHOLD;
    }

    public boolean shouldEvictTrending(long count){
        return count>= TRENDING_THRESHOLD;
    }

    public boolean shouldEvictNew(long count){
        return count>= NEW_THRESHOLD;
    }


}
