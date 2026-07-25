package com.jatin.forum.service;

import com.jatin.forum.dto.CachedFeed;
import com.jatin.forum.dto.CachedPost;
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
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;


import java.time.temporal.ChronoUnit;
import java.util.*;


@Service
@Slf4j
public class PostService {
    private final UserRepo userRepo;
    private final PostVoteRepo postVoteRepo;
    private final CommentRepo commentRepo;
    private final PostRepo postRepo;
    private final RedisTemplate<String,String> redisTemplate;
    private final FeedCacheService feedCacheService;
    @Autowired
    private ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final PostMapper postMapper;

    public PostService(PostRepo postRepo, UserRepo userRepo, PostVoteRepo postVoteRepo, CommentRepo commentRepo, RedisTemplate<String,String> redisTemplate, FeedCacheService feedCacheService, CurrentUserService currentUserService, PostMapper postMapper) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
        this.postVoteRepo = postVoteRepo;
        this.commentRepo = commentRepo;
        this.redisTemplate = redisTemplate;
        this.feedCacheService = feedCacheService;
        this.currentUserService = currentUserService;
        this.postMapper = postMapper;
    }



    public PostFeedResponse getAllPosts(String sort, int page,int  limit, String cursor){
             User user = currentUserService.getCurrentUser().orElse(null);
             if(sort.equals("new")){
                 // newSections: first page is almost the same for everyone: and can be same for two person for specific time
                 // Rest of the pages can be fetched from databases directly
                 if(cursor==null){
                     // first page,load from cache, and cache reloads from time to time
                     Optional<CachedFeed> cachedPostFeed = feedCacheService.getCachedFeed(FeedCacheService.TYPE_NEW, limit);
                     if(cachedPostFeed.isPresent()){ // if cache is present-> fetch it
                         // fetch from cache
                         // we have to create a PostFeedResponse
                         // only need VoteType for specific user
                         CachedFeed cachedFeed = cachedPostFeed.get();

                            List<Long> postIds = new ArrayList<>();
                            for(CachedPost post:cachedFeed.cachedPostList()){
                                postIds.add(post.id());
                            }

                            HashMap<Long,VoteType> voteMap = this.getVoteTypeHashMap(user,postIds);
                            HashMap<Long, Long> voteCountMap = this.getVoteCountHashMap(postIds);

                         List<PostResponse> postFeedResponseList = cachedFeed.cachedPostList().stream().map(cachedPost -> postMapper.mapToPostResponseFromCachePost(cachedPost,voteMap,voteCountMap)).toList();
                         boolean hasMore = cachedFeed.hasMore();
                         String newCursor = cachedFeed.cursor()==null? null : cachedFeed.cursor().toString();
                         return new PostFeedResponse(postFeedResponseList,newCursor,hasMore);
                     }
                     // first page->not stored in cache->fetch from database->store in cache
                     Instant cursorTime = Instant.now();

                     List<Post> newList = postRepo.findPostNew(cursorTime, PageRequest.of(0, limit+1));

                     boolean hasMore = newList.size()>limit;
                     if(hasMore){
                         // drop a post
                         newList.remove(newList.size()-1);
                     }
                     // removing the query finding VoteType by user and post for each post
                     List<Long> postIds = new ArrayList<>();
                     for(Post post:newList){
                         postIds.add(post.getId());
                     }

                    HashMap<Long,VoteType> voteTypeHashMap = this.getVoteTypeHashMap(user,postIds);
                    HashMap<Long, Long> voteCountMap = this.getVoteCountHashMap(postIds);

                     List<PostResponse> postResponses = newList.stream()
                             .map(post -> postMapper.mapToPostResponse(post,voteTypeHashMap,voteCountMap))
                             .toList();

                     Instant newCursor = postResponses.isEmpty() ? null : postResponses.get(postResponses.size() - 1).createdAt();

                     // store cache whether authenticated or not
                     List<CachedPost> cachedPostList = newList.stream().map(postMapper::mapToCachedPostFromPost).toList();
                     feedCacheService.setCachedFeed(FeedCacheService.TYPE_NEW, cachedPostList, hasMore, newCursor, limit);
                     return new PostFeedResponse(postResponses, newCursor != null ? newCursor.toString() : null, hasMore);
                 }
                 else{

                     // cursor is not null->fetch normally-> no cache ,cause cursor can be different for different people
                     Instant cursorTime = Instant.parse(cursor);
                     List<Post> newList = postRepo.findPostNew(cursorTime, PageRequest.of(0, limit+1));

                     // removing the query finding VoteType by user and post for each post
                     List<Long> postIds = new ArrayList<>();
                     for(Post post:newList){
                         postIds.add(post.getId());
                     }

                     // select PostVote from repo where user = user and postID IN(1,2,3,4,...) -> Ine one operation gets all votetypes
                     // for all posts
                     HashMap<Long,VoteType> voteTypeHashMap = this.getVoteTypeHashMap(user,postIds);
                     HashMap<Long, Long> voteCountMap = this.getVoteCountHashMap(postIds);

                     List<PostResponse> postResponses = newList.stream()
                             .map(post -> postMapper.mapToPostResponse(post,voteTypeHashMap,voteCountMap))
                             .toList();
                     Instant newCursor = postResponses.isEmpty() ? null : postResponses.get(postResponses.size() - 1).createdAt();
                     boolean hasMore = newList.size() == limit;
                     return new PostFeedResponse(postResponses, newCursor != null ? newCursor.toString() : null, hasMore);
                 }

             }
             if(sort.equals("hot")) {
                 // check cache keys->
                  // only 0th page is being cached,so only it should be fetched
                 if(page==0){
                     Optional<CachedFeed> cachedFeed = feedCacheService.getCachedFeed(FeedCacheService.TYPE_HOT, limit);
                     if(cachedFeed.isPresent()){ // if not null->return cache
                       CachedFeed cachedFeedFinal = cachedFeed.get();
                       List<Long> postIds = new ArrayList<>();
                       for(CachedPost post:cachedFeedFinal.cachedPostList()) {
                         postIds.add(post.id());
                       }
                       HashMap<Long, VoteType> voteTypeHashMap = this.getVoteTypeHashMap(user, postIds);
                       HashMap<Long, Long> voteCountMap = this.getVoteCountHashMap(postIds);
                       List<PostResponse> postResponsesList = cachedFeedFinal.cachedPostList().stream().map(post -> postMapper.mapToPostResponseFromCachePost(post,voteTypeHashMap,voteCountMap)).toList();
                       boolean hasMore = cachedFeedFinal.hasMore();
                       String newCursor = cachedFeedFinal.cursor()==null ? null : cachedFeedFinal.cursor().toString();
                       return new  PostFeedResponse(postResponsesList, newCursor, hasMore);
                     }
                 }
                         // if page is not zero or if cache doesn't exist:
                         // cache is null
                         // hit the database
                         Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
                         List<Post> hotList = postRepo.findPostRecent(sevenDaysAgo);

                         List<Long> postIds = new ArrayList<>();
                         for(Post post:hotList){
                             postIds.add(post.getId());
                         }
                         HashMap<Long,VoteType> voteTypeHashMap = this.getVoteTypeHashMap(user,postIds);
                         HashMap<Long, Long> voteCountMap = this.getVoteCountHashMap(postIds);

                         List<Post> hotFeedPage = hotList.stream().sorted(Comparator.comparing(postMapper::getHotScorePost).reversed())
                                 .skip((long)page*limit)
                                 .limit(limit)
                                 .toList();

                         List<PostResponse> postResponseList = hotFeedPage.stream().map(post -> postMapper.mapToPostResponse(post,voteTypeHashMap,voteCountMap)).toList();
                         boolean hasMore = hotList.size() > (page+1)*limit;
                         PostFeedResponse postFeedResponse = new PostFeedResponse(postResponseList, null, hasMore);
                         // setTheFeed cache
                         List<CachedPost> cachedPostList = hotFeedPage.stream().map(postMapper::mapToCachedPostFromPost).toList();
                         if(!(page > 0)){ // wont cache future pages
                             feedCacheService.setCachedFeed(FeedCacheService.TYPE_HOT, cachedPostList, hasMore, null, limit);
                         }
                         return postFeedResponse;
             }



             if(sort.equals("trending")) {
                 if(page==0){
                   Optional<CachedFeed> cachedFeedOptional = feedCacheService.getCachedFeed(FeedCacheService.TYPE_TRENDING, limit);
                   if(cachedFeedOptional.isPresent()){
                      CachedFeed cachedFeed = cachedFeedOptional.get();
                      ArrayList<Long> postIds = new  ArrayList<>();
                      for(CachedPost cachedPost:cachedFeed.cachedPostList()){
                          postIds.add(cachedPost.id());
                      }
                      HashMap<Long,VoteType> voteTypeHashMap = this.getVoteTypeHashMap(user,postIds);
                      HashMap<Long, Long> voteCountMap = this.getVoteCountHashMap(postIds);

                     List<PostResponse> postResponses = cachedFeed.cachedPostList().stream().map(post -> postMapper.mapToPostResponseFromCachePost(post,voteTypeHashMap,voteCountMap)).toList();

                     boolean hasMore = cachedFeed.hasMore();
                     String newCursor = cachedFeed.cursor() == null ? null : cachedFeed.cursor().toString();
                     return new PostFeedResponse(postResponses, null, hasMore);
                   }
                }
                     Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
                     List<Post> recentTotal = postRepo.findPostRecent(twentyFourHoursAgo);

                         // removing the query finding VoteType by user and post for each post
                         List<Long> postIds = new ArrayList<>();
                         for(Post post:recentTotal){
                             postIds.add(post.getId());
                         }

                     HashMap<Long,VoteType> voteTypeHashMap = this.getVoteTypeHashMap(user,postIds);
                     HashMap<Long, Long> voteCountMap = this.getVoteCountHashMap(postIds);

                     List<Post> trendingFeedPage = recentTotal
                             .stream()
                             .sorted((p1,p2)->Double.compare(postMapper.getTrendingScore(p2),postMapper.getTrendingScore(p1)))
                             .skip((long)page*limit)
                             .limit(limit)
                             .toList();

                     List<PostResponse> postResponses = trendingFeedPage.stream().map(post -> postMapper.mapToPostResponse(post,voteTypeHashMap,voteCountMap)).toList();
                     boolean hasMore = recentTotal.size() > (page+1)*limit;
                     PostFeedResponse postFeedResponse = new  PostFeedResponse(postResponses, null, hasMore);
                     List<CachedPost> cachedPostList = trendingFeedPage.stream().map(postMapper::mapToCachedPostFromPost).toList();
                     if(!(page>0)) {
                         feedCacheService.setCachedFeed(FeedCacheService.TYPE_TRENDING, cachedPostList, hasMore, null, limit);
                     }
                     return postFeedResponse;

             }
             return new PostFeedResponse(List.of(), null, false);
    }

    private HashMap<Long,VoteType> getVoteTypeHashMap(User user,List<Long> postIds){
        if(user==null){
            return new  HashMap<>();
        }
        if(postIds == null || postIds.isEmpty()){
            return new HashMap<>();
        }
        List<PostVote> postVotes = postVoteRepo.findByUserAndPostIdIn(user,postIds);
        HashMap<Long,VoteType> voteTypeHashMap = new HashMap<>();
        for(PostVote postVote:postVotes){
            voteTypeHashMap.put(postVote.getPost().getId(), postVote.getVoteType());
        }
        return voteTypeHashMap;
    }

    private HashMap<Long, Long> getVoteCountHashMap(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Object[]> results = postVoteRepo.getAggregateVotesForPosts(postIds);
        HashMap<Long, Long> countMap = new HashMap<>();
        for (Object[] result : results) {
            Long postId = (Long) result[0];
            Number count = (Number) result[1];
            countMap.put(postId, count != null ? count.longValue() : 0L);
        }
        return countMap;
    }




    public PostResponse createPost(String title, String content, String mediaUrl, String mediaType, String mediaPublicId){
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));
        Post post = new Post(title, content, user);
        post.setMediaUrl(mediaUrl);
        post.setMediaType(mediaType);
        post.setMediaPublicId(mediaPublicId);

        Post savedPost = postRepo.save(post);

        feedCacheService.incrementActivity(FeedCacheService.TYPE_NEW);

            feedCacheService.evictFeed(FeedCacheService.TYPE_NEW, 10);
            feedCacheService.resetActivity(FeedCacheService.TYPE_NEW);

            // increment counter for hot and trending keys
            feedCacheService.incrementActivity(FeedCacheService.TYPE_TRENDING);
            feedCacheService.incrementActivity(FeedCacheService.TYPE_HOT);

        HashMap<Long,VoteType> map = new HashMap<>();
        HashMap<Long, Long> voteCountMap = new HashMap<>();
        return postMapper.mapToPostResponse(savedPost,map,voteCountMap);
    }

    public PostResponse getPostById(Long id){
        Post post = postRepo.findById(id).orElseThrow(()-> {
            return new RuntimeException("post not found");
        });
        HashMap<Long,VoteType> voteTypeHashMap = new HashMap<>();
        HashMap<Long, Long> voteCountMap = this.getVoteCountHashMap(Collections.singletonList(id));
        User user = currentUserService.getCurrentUser().orElse(null);
        if (user != null) {
             Optional<PostVote> postVote = postVoteRepo.findByUserAndPost(user,post);
            postVote.ifPresent(vote -> voteTypeHashMap.put(id, vote.getVoteType()));
             return postMapper.mapToPostResponse(post,voteTypeHashMap,voteCountMap);
        } else {
             return postMapper.mapToPostResponse(post,voteTypeHashMap,voteCountMap);
        }
    }

    @Transactional
    public void deletePostById(Long postId){
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));
        Post post = postRepo.findById(postId).orElseThrow(()-> {
            return new RuntimeException("post not found");
        });
        if(!post.getUser().getId().equals(user.getId())){
           throw new RuntimeException("Not allowed to delete post");
        }
        postVoteRepo.deleteByPostId(postId);
        commentRepo.deleteByPostId(postId);
        postRepo.deleteById(postId);

        feedCacheService.evictFeed(FeedCacheService.TYPE_NEW, 10);
        feedCacheService.resetActivity(FeedCacheService.TYPE_NEW);
        feedCacheService.evictFeed(FeedCacheService.TYPE_HOT, 10);
        feedCacheService.resetActivity(FeedCacheService.TYPE_HOT);
        feedCacheService.evictFeed(FeedCacheService.TYPE_TRENDING, 10);
        feedCacheService.resetActivity(FeedCacheService.TYPE_TRENDING);

    }



}
