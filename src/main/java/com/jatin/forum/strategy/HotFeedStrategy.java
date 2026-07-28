package com.jatin.forum.strategy;

import com.jatin.forum.dto.CachedFeed;
import com.jatin.forum.dto.CachedPost;
import com.jatin.forum.dto.PostFeedResponse;
import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.service.FeedCacheService;
import com.jatin.forum.service.PostMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component("hot")
public class HotFeedStrategy implements FeedStrategy{

    private final FeedCacheService feedCacheService;
    private final PostMapper postMapper;
    private final PostRepo postRepo;

    public HotFeedStrategy(FeedCacheService feedCacheService, PostMapper postMapper, PostRepo postRepo) {
        this.feedCacheService = feedCacheService;
        this.postMapper = postMapper;
        this.postRepo = postRepo;
    }


    @Override
    public PostFeedResponse fetchFeed(User user, int page, int limit, String cursor) {
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
                HashMap<Long, VoteType> voteTypeHashMap = postMapper.getVoteTypeHashMap(user, postIds);
                HashMap<Long, Long> voteCountMap = postMapper.getVoteCountHashMap(postIds);
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
        HashMap<Long,VoteType> voteTypeHashMap = postMapper.getVoteTypeHashMap(user,postIds);
        HashMap<Long, Long> voteCountMap = postMapper.getVoteCountHashMap(postIds);

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
}
