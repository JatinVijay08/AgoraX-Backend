package com.jatin.forum.service;

import com.jatin.forum.dto.PostFeedResponse;
import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.Post;

import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;


import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class PostService {
    private final UserRepo userRepo;
    private final PostVoteRepo postVoteRepo;
    private final CommentRepo commentRepo;
    private PostRepo postRepo;
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    tools.jackson.databind.ObjectMapper jacksonObjectMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public PostService(PostRepo postRepo, UserRepo userRepo, PostVoteRepo postVoteRepo, CommentRepo commentRepo,RedisTemplate<String,String> redisTemplate) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
        this.postVoteRepo = postVoteRepo;
        this.commentRepo = commentRepo;
        this.redisTemplate = redisTemplate;
    }



    public PostFeedResponse getAllPosts(String sort, int page,int  limit, String cursor){
             Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
             boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser");
             log.info("[SERVICE] Fetching posts list. Sort: {}, Page: {}, Limit: {}, Cursor: {}, Authenticated User: {}", sort, page, limit, cursor, isAuthenticated ? authentication.getName() : "Anonymous");

             if(sort.equals("new")){
                 // newSections: first page is almost the same for everyone: and can be same for two person for specific time
                 // Rest of the pages can be fetched from databases directly
                 if(cursor==null){
                     // first page,load from cache, and cache reloads from time to time
                     String key = "feed:"+"new:"+limit;
                     log.info("[SERVICE] Attempting to hit Redis cache for key: {}", key);
                     String cached =  redisTemplate.opsForValue().get(key);
                     if(cached!=null){
                         log.info("[SERVICE] Redis Cache HIT for key: {}", key);
                         // fetch from cache
                         // deserialize from JSON String
                         return objectMapper.readValue(cached,PostFeedResponse.class);
                     }
                     log.info("[SERVICE] Redis Cache MISS for key: {}. Fetching from Database...", key);
                     // first page->not stored in cache->fetch from database->store in cache
                     Instant cursorTime = Instant.now();
                     List<Post> newList = postRepo.findPostNew(cursorTime, PageRequest.of(0, limit));
                     List<PostResponse> postResponses = newList.stream()
                             .map(post -> isAuthenticated ? mapToPostResponse(post) : mapToPostResponseUnauthenticated(post))
                             .toList();
                     Instant newCursor = postResponses.isEmpty() ? null : postResponses.get(postResponses.size() - 1).createdAt();
                     boolean hasMore = newList.size() == limit;
                     PostFeedResponse postFeedResponse = new PostFeedResponse(postResponses, newCursor != null ? newCursor.toString() : null, hasMore);

                         String cacheStringObject = objectMapper.writeValueAsString(postFeedResponse);
                         redisTemplate.opsForValue().set(key, cacheStringObject, 1, TimeUnit.MINUTES);
                         log.info("[SERVICE] Saved retrieved feed to Redis cache with key: {}", key);

                     return postFeedResponse;
                 }
                 else{
                     // cursor is not null->fetch normally-> no cache ,cause cursor can be different for different people
                     log.info("[SERVICE] Cursor is present: {}. Fetching new posts from database directly...", cursor);
                     Instant cursorTime = Instant.parse(cursor);
                     List<Post> newList = postRepo.findPostNew(cursorTime, PageRequest.of(0, limit));
                     List<PostResponse> postResponses = newList.stream()
                             .map(post -> isAuthenticated ? mapToPostResponse(post) : mapToPostResponseUnauthenticated(post))
                             .toList();
                     Instant newCursor = postResponses.isEmpty() ? null : postResponses.get(postResponses.size() - 1).createdAt();
                     boolean hasMore = newList.size() == limit;
                     return new PostFeedResponse(postResponses, newCursor != null ? newCursor.toString() : null, hasMore);
                 }

             }
             if(sort.equals("hot")) {
                 String key = "feed:"+"hot:"+1;
                 log.info("[SERVICE] Attempting to hit Redis cache for key: {}", key);
                 // check cache keys->
                 String cached =  redisTemplate.opsForValue().get(key);
                 if(cached!=null){
                     log.info("[SERVICE] Redis Cache HIT for key: {}", key);
                     return objectMapper.readValue(cached,PostFeedResponse.class);
                 }else{
                     log.info("[SERVICE] Redis Cache MISS for key: {}. Querying database for hot posts...", key);
                     // cache is null
                     // hit the database
                     Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
                     List<Post> hotList = postRepo.findPostRecent(sevenDaysAgo);
                     List<PostResponse> postResponses = hotList.stream()
                             .map(post -> isAuthenticated ? mapToPostResponse(post) : mapToPostResponseUnauthenticated(post))
                             .sorted(Comparator.comparingDouble(PostResponse::hotScore).reversed())
                             .skip((long) page * limit)
                             .limit(limit)
                             .toList();
                     boolean hasMore = postResponses.size() == limit;
                    PostFeedResponse postFeedResponse = new PostFeedResponse(postResponses, null, hasMore);
                    String cacheStringObject = objectMapper.writeValueAsString(postFeedResponse);
                    int jitter = ThreadLocalRandom.current().nextInt(0, 30);
                    redisTemplate.opsForValue().set(key, cacheStringObject, 120+jitter, TimeUnit.SECONDS);
                    log.info("[SERVICE] Saved hot feed results to Redis cache with key: {}", key);
                    return postFeedResponse;
                 }
             }

             if(sort.equals("trending")) {

                 String key = "feed:"+"trending:"+1;
                 log.info("[SERVICE] Attempting to hit Redis cache for key: {}", key);
                 String cached =  redisTemplate.opsForValue().get(key);
                 if(cached!=null){
                     log.info("[SERVICE] Redis Cache HIT for key: {}", key);
                     return objectMapper.readValue(cached,PostFeedResponse.class);
                 }
                 else{
                     log.info("[SERVICE] Redis Cache MISS for key: {}. Querying database for trending posts...", key);
                     Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
                     List<Post> recentTotal = postRepo.findPostRecent(twentyFourHoursAgo);

                     List<PostResponse> postResponses = recentTotal.stream()
                             .sorted((p1, p2) -> Double.compare(getTrendingScore(p2), getTrendingScore(p1)))
                             .skip((long) page * limit)
                             .limit(limit)
                             .map(post -> isAuthenticated ? mapToPostResponse(post) : mapToPostResponseUnauthenticated(post))
                             .toList();

                     boolean hasMore = postResponses.size() == limit;
                     PostFeedResponse postFeedResponse = new  PostFeedResponse(postResponses, null, hasMore);
                     String cachedObjectString = objectMapper.writeValueAsString(postFeedResponse);
                     int jitter = ThreadLocalRandom.current().nextInt(0, 30);
                     redisTemplate.opsForValue().set(key, cachedObjectString, 120+jitter, TimeUnit.SECONDS);
                     log.info("[SERVICE] Saved trending feed results to Redis cache with key: {}", key);
                     return postFeedResponse;
                 }

             }
             return new PostFeedResponse(List.of(), null, false);
    }

    private double getTrendingScore(Post post) {
        Instant sixHoursAgo = Instant.now().minus(6, ChronoUnit.HOURS);
        long recentVotes = postVoteRepo.countByPostAndCreatedAtAfter(post, sixHoursAgo);
        long recentComments = commentRepo.countByPostAndCreatedAtAfter(post, sixHoursAgo);
        return recentVotes + (recentComments * 2);
    }

    public PostResponse createPost(String title, String content, String mediaUrl, String mediaType, String mediaPublicId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("[SERVICE] Creating post for user email: {}, title: {}", email, title);
        User user = userRepo.findByEmail(email);
        if(user==null){
            log.error("[SERVICE] Post creation failed: User not found with email: {}", email);
            throw new RuntimeException("User not found");
        }
        Post post = new Post(title, content, user);
        post.setMediaUrl(mediaUrl);
        post.setMediaType(mediaType);
        post.setMediaPublicId(mediaPublicId);

        Post savedPost = postRepo.save(post);
        log.info("[SERVICE] Post saved in database with ID: {}", savedPost.getId());

        log.info("[SERVICE] Clearing feed cache from Redis...");
        Set<String> keys = redisTemplate.keys("feed:*");
        if(keys!=null && !keys.isEmpty()){
            redisTemplate.delete(keys);
            log.info("[SERVICE] Deleted {} cached keys matching feed:*", keys.size());
        }
        return mapToPostResponse(savedPost);
    }

    public PostResponse getPostById(Long id){
        log.info("[SERVICE] Fetching post from DB with ID: {}", id);
        Post post = postRepo.findById(id).orElseThrow(()-> {
            log.warn("[SERVICE] Post ID {} not found in database", id);
            return new RuntimeException("post not found");
        });
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
             log.info("[SERVICE] User is authenticated as: {}. Generating mapToPostResponse", authentication.getName());
             return mapToPostResponse(post);
        } else {
             log.info("[SERVICE] Request is unauthenticated. Generating mapToPostResponseUnauthenticated");
             return mapToPostResponseUnauthenticated(post);
        }
    }

    @Transactional
    public void deletePostById(Long postId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("[SERVICE] Deleting post ID: {} initiated by: {}", postId, email);
        User user = userRepo.findByEmail(email);
        if(user==null){
            log.error("[SERVICE] Delete post failed: User not found with email {}", email);
            throw new RuntimeException("User not found");
        }
        Post post = postRepo.findById(postId).orElseThrow(()-> {
            log.warn("[SERVICE] Post ID {} not found during delete attempt", postId);
            return new RuntimeException("post not found");
        });
        if(!post.getUser().getId().equals(user.getId())){
           log.warn("[SERVICE] Delete post failed: User ID {} is not allowed to delete post ID {} owned by User ID {}", user.getId(), postId, post.getUser().getId());
           throw new RuntimeException("Not allowed to delete post");
        }
        log.info("[SERVICE] Deleting post votes, post comments, and the post itself for ID: {}", postId);
        postVoteRepo.deleteByPostId(postId);
        commentRepo.deleteByPostId(postId);
        postRepo.deleteById(postId);
        log.info("[SERVICE] Post ID {} and all associated entities successfully deleted", postId);

    }

    public PostResponse mapToPostResponse(Post post){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if(user==null){
            throw new RuntimeException("User not found");
        }
        long upvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.upvote);
        long downvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.downvote);

        long votes = upvotes-downvotes;
        long commentCount = commentRepo.countByPostId(post.getId());
        VoteType voteType = postVoteRepo.findByUserAndPost(user,post).map(PostVote::getVoteType).orElse(null);
        User user1 = post.getUser();
        long hoursOld = Duration.between(post.getCreatedAt(),Instant.now()).toHours();
        double hotScore = votes/Math.pow(hoursOld+2,1.5);

        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType,post.getCreatedAt(),hotScore, post.getMediaUrl(), post.getMediaType());

    }

    public PostResponse mapToPostResponseUnauthenticated(Post post){
        long upvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.upvote);
        long downvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.downvote);

        long votes = upvotes-downvotes;
        long commentCount = commentRepo.countByPostId(post.getId());
        VoteType voteType = null;
        User user1 = post.getUser();
        long hoursOld = Duration.between(post.getCreatedAt(),Instant.now()).toHours();
        double hotScore = votes/Math.pow(hoursOld+2,1.5);

        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType,post.getCreatedAt(),hotScore, post.getMediaUrl(), post.getMediaType());
    }

}
