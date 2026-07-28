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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Component("trending")
public class TrendingFeedStrategy implements FeedStrategy{
    private final FeedCacheService feedCacheService;
    private final PostMapper postMapper;
    private final PostRepo postRepo;

    public TrendingFeedStrategy(FeedCacheService feedCacheService, PostMapper postMapper, PostRepo postRepo) {
        this.feedCacheService = feedCacheService;
        this.postMapper = postMapper;
        this.postRepo = postRepo;
    }

    @Override
    public PostFeedResponse fetchFeed(User user, int page, int limit, String cursor) {
        if(page==0){
            Optional<CachedFeed> cachedFeedOptional = feedCacheService.getCachedFeed(FeedCacheService.TYPE_TRENDING, limit);
            if(cachedFeedOptional.isPresent()){
                CachedFeed cachedFeed = cachedFeedOptional.get();
                ArrayList<Long> postIds = new  ArrayList<>();
                for(CachedPost cachedPost:cachedFeed.cachedPostList()){
                    postIds.add(cachedPost.id());
                }
                HashMap<Long, VoteType> voteTypeHashMap = postMapper.getVoteTypeHashMap(user,postIds);
                HashMap<Long, Long> voteCountMap = postMapper.getVoteCountHashMap(postIds);

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

        HashMap<Long,VoteType> voteTypeHashMap = postMapper.getVoteTypeHashMap(user,postIds);
        HashMap<Long, Long> voteCountMap = postMapper.getVoteCountHashMap(postIds);

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
}
