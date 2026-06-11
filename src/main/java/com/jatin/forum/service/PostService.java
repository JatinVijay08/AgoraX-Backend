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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;


import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class PostService {
    private final UserRepo userRepo;
    private final PostVoteRepo postVoteRepo;
    private final CommentRepo commentRepo;
    private final PostRepo postRepo;
    private final RedisTemplate<String,String> redisTemplate;
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
        assert authentication != null;
        String email = authentication.getName();
             if(sort.equals("new")){
                 // newSections: first page is almost the same for everyone: and can be same for two person for specific time
                 // Rest of the pages can be fetched from databases directly
                 if(cursor==null){
                     // first page,load from cache, and cache reloads from time to time
                     String key = "feed:"+"new:"+limit;

                     long start = System.currentTimeMillis();
                     String cached =  redisTemplate.opsForValue().get(key);
                     System.out.println("Redis get request time:   "+(System.currentTimeMillis()-start));

                     if(cached!=null){
                         // fetch from cache
                         // deserialize from JSON String
                         return objectMapper.readValue(cached,PostFeedResponse.class);
                     }
                     // first page->not stored in cache->fetch from database->store in cache
                     Instant cursorTime = Instant.now();

                     List<Post> newList = postRepo.findPostNew(cursorTime, PageRequest.of(0, limit));

                     User user = userRepo.findByEmail(email);

                     // removing the query finding VoteType by user and post for each post
                     List<Long> postIds = new ArrayList<>();
                     for(Post post:newList){
                         postIds.add(post.getId());
                     }

                     // select PostVote from repo where user = user and postID IN(1,2,3,4,...) -> Ine one operation gets all votetypes
                     // for all posts
                     List<PostVote> postVotes = postVoteRepo.findByUserAndPostIdIn(user,postIds);
                     HashMap<Long,VoteType> voteTypeHashMap = new HashMap<>();

                     for(PostVote vote:postVotes){
                         voteTypeHashMap.put(vote.getPost().getId(), vote.getVoteType());
                     }

                     List<PostResponse> postResponses = newList.stream()
                             .map(post -> isAuthenticated ? mapToPostResponse(post,user,voteTypeHashMap) : mapToPostResponseUnauthenticated(post))
                             .toList();

                     Instant newCursor = postResponses.isEmpty() ? null : postResponses.get(postResponses.size() - 1).createdAt();
                     boolean hasMore = newList.size() == limit;
                     PostFeedResponse postFeedResponse = new PostFeedResponse(postResponses, newCursor != null ? newCursor.toString() : null, hasMore);

                         String cacheStringObject = objectMapper.writeValueAsString(postFeedResponse);

                     redisTemplate.opsForValue().set(key, cacheStringObject, 1, TimeUnit.MINUTES);

                     return postFeedResponse;
                 }
                 else{

                     // cursor is not null->fetch normally-> no cache ,cause cursor can be different for different people
                     Instant cursorTime = Instant.parse(cursor);
                     List<Post> newList = postRepo.findPostNew(cursorTime, PageRequest.of(0, limit));
                     User user = userRepo.findByEmail(email);

                     // removing the query finding VoteType by user and post for each post
                     List<Long> postIds = new ArrayList<>();
                     for(Post post:newList){
                         postIds.add(post.getId());
                     }

                     // select PostVote from repo where user = user and postID IN(1,2,3,4,...) -> Ine one operation gets all votetypes
                     // for all posts
                     List<PostVote> postVotes = postVoteRepo.findByUserAndPostIdIn(user,postIds);
                     HashMap<Long,VoteType> voteTypeHashMap = new HashMap<>();

                     for(PostVote vote:postVotes){
                         voteTypeHashMap.put(vote.getPost().getId(), vote.getVoteType());
                     }

                     List<PostResponse> postResponses = newList.stream()
                             .map(post -> isAuthenticated ? mapToPostResponse(post,user,voteTypeHashMap) : mapToPostResponseUnauthenticated(post))
                             .toList();
                     Instant newCursor = postResponses.isEmpty() ? null : postResponses.get(postResponses.size() - 1).createdAt();
                     boolean hasMore = newList.size() == limit;
                     return new PostFeedResponse(postResponses, newCursor != null ? newCursor.toString() : null, hasMore);
                 }

             }
             if(sort.equals("hot")) {
                 String key = "feed:"+"hot:"+limit;
                 // check cache keys->
                 String cached =  redisTemplate.opsForValue().get(key);
                 if(cached!=null){
                     return objectMapper.readValue(cached,PostFeedResponse.class);
                 }else{
                     // cache is null
                     // hit the database
                     Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
                     List<Post> hotList = postRepo.findPostRecent(sevenDaysAgo);
                     User user = userRepo.findByEmail(email);

                     // removing the query finding VoteType by user and post for each post
                     List<Long> postIds = new ArrayList<>();
                     for(Post post:hotList){
                         postIds.add(post.getId());
                     }

                     // select PostVote from repo where user = user and postID IN(1,2,3,4,...) -> Ine one operation gets all votetypes
                     // for all posts
                     List<PostVote> postVotes = postVoteRepo.findByUserAndPostIdIn(user,postIds);
                     HashMap<Long,VoteType> voteTypeHashMap = new HashMap<>();

                     for(PostVote vote:postVotes){
                         voteTypeHashMap.put(vote.getPost().getId(), vote.getVoteType());
                     }

                     List<PostResponse> postResponses = hotList.stream()
                             .map(post -> isAuthenticated ? mapToPostResponse(post,user,voteTypeHashMap) : mapToPostResponseUnauthenticated(post))
                             .sorted(Comparator.comparingDouble(PostResponse::hotScore).reversed())
                             .skip((long) page * limit)
                             .limit(limit)
                             .toList();
                     boolean hasMore = postResponses.size() == limit;
                    PostFeedResponse postFeedResponse = new PostFeedResponse(postResponses, null, hasMore);
                    String cacheStringObject = objectMapper.writeValueAsString(postFeedResponse);
                    int jitter = ThreadLocalRandom.current().nextInt(0, 30);
                    redisTemplate.opsForValue().set(key, cacheStringObject, 120+jitter, TimeUnit.SECONDS);
                    return postFeedResponse;
                 }
             }

             if(sort.equals("trending")) {

                 String key = "feed:"+"trending:"+limit;
                 String cached =  redisTemplate.opsForValue().get(key);
                 if(cached!=null){
                     return objectMapper.readValue(cached,PostFeedResponse.class);
                 }
                 else{
                     Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
                     List<Post> recentTotal = postRepo.findPostRecent(twentyFourHoursAgo);

                     User user = userRepo.findByEmail(email);

                     // removing the query finding VoteType by user and post for each post
                     List<Long> postIds = new ArrayList<>();
                     for(Post post:recentTotal){
                         postIds.add(post.getId());
                     }

                     // select PostVote from repo where user = user and postID IN(1,2,3,4,...) -> Ine one operation gets all votetypes
                     // for all posts
                     List<PostVote> postVotes = postVoteRepo.findByUserAndPostIdIn(user,postIds);
                     HashMap<Long,VoteType> voteTypeHashMap = new HashMap<>();

                     for(PostVote vote:postVotes){
                         voteTypeHashMap.put(vote.getPost().getId(), vote.getVoteType());
                     }

                     List<PostResponse> postResponses = recentTotal.stream()
                             .sorted((p1, p2) -> Double.compare(getTrendingScore(p2), getTrendingScore(p1)))
                             .skip((long) page * limit)
                             .limit(limit)
                             .map(post -> isAuthenticated ? mapToPostResponse(post,user,voteTypeHashMap) : mapToPostResponseUnauthenticated(post))
                             .toList();

                     boolean hasMore = postResponses.size() == limit;
                     PostFeedResponse postFeedResponse = new  PostFeedResponse(postResponses, null, hasMore);
                     String cachedObjectString = objectMapper.writeValueAsString(postFeedResponse);
                     int jitter = ThreadLocalRandom.current().nextInt(0, 30);
                     redisTemplate.opsForValue().set(key, cachedObjectString, 120+jitter, TimeUnit.SECONDS);
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
        assert authentication != null;
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if(user==null){
            throw new RuntimeException("User not found");
        }
        Post post = new Post(title, content, user);
        post.setMediaUrl(mediaUrl);
        post.setMediaType(mediaType);
        post.setMediaPublicId(mediaPublicId);

        Post savedPost = postRepo.save(post);

        Set<String> keys = redisTemplate.keys("feed:*");
        if(keys!=null && !keys.isEmpty()){
            redisTemplate.delete(keys);
        }
        HashMap<Long,VoteType> map = new HashMap<>();
        return mapToPostResponse(savedPost,user,map);
    }

    public PostResponse getPostById(Long id){
        Post post = postRepo.findById(id).orElseThrow(()-> {
            return new RuntimeException("post not found");
        });
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String email = authentication.getName();
            User user = userRepo.findByEmail(email);
             Optional<PostVote> postVote = postVoteRepo.findByUserAndPost(user,post);
             HashMap<Long,VoteType> voteTypeHashMap = new HashMap<>();
            postVote.ifPresent(vote -> voteTypeHashMap.put(id, vote.getVoteType()));
             return mapToPostResponse(post,user,voteTypeHashMap);
        } else {
             return mapToPostResponseUnauthenticated(post);
        }
    }

    @Transactional
    public void deletePostById(Long postId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if(user==null){
            throw new RuntimeException("User not found");
        }
        Post post = postRepo.findById(postId).orElseThrow(()-> {
            return new RuntimeException("post not found");
        });
        if(!post.getUser().getId().equals(user.getId())){
           throw new RuntimeException("Not allowed to delete post");
        }
        postVoteRepo.deleteByPostId(postId);
        commentRepo.deleteByPostId(postId);
        postRepo.deleteById(postId);

    }

    public PostResponse mapToPostResponse(Post post,User user,HashMap<Long ,VoteType> voteTypeHashMap){

        if(user==null){
            throw new RuntimeException("User not found");
        }


        long upvotes = post.getUpvotesCount();


        long downvotes = post.getDownvotesCount();

        long votes = upvotes-downvotes;

        long commentCount = post.getCommentCount();


        com.jatin.forum.entity.VoteType voteType = voteTypeHashMap.getOrDefault(post.getId(), null);

        User user1 = post.getUser();

        long hoursOld = Duration.between(post.getCreatedAt(),Instant.now()).toHours();
        double hotScore = votes/Math.pow(hoursOld+2,1.5);

        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType,post.getCreatedAt(),hotScore, post.getMediaUrl(), post.getMediaType());

    }

    public PostResponse mapToPostResponseUnauthenticated(Post post){
        long upvotes = post.getUpvotesCount();
        long downvotes = post.getDownvotesCount();

        long votes = upvotes-downvotes;
        long commentCount = post.getCommentCount();
        User user1 = post.getUser();
        long hoursOld = Duration.between(post.getCreatedAt(),Instant.now()).toHours();
        double hotScore = votes/Math.pow(hoursOld+2,1.5);

        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, null,post.getCreatedAt(),hotScore, post.getMediaUrl(), post.getMediaType());
    }

}
