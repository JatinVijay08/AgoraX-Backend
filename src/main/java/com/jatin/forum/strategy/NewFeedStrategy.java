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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Component("new")
public class NewFeedStrategy implements FeedStrategy {
    private final FeedCacheService feedCacheService;
    private final PostMapper postMapper;
    private final PostRepo postRepo;

    public NewFeedStrategy(FeedCacheService feedCacheService, PostMapper postMapper, PostRepo postRepo){
        this.feedCacheService = feedCacheService;
        this.postMapper = postMapper;
        this.postRepo = postRepo;
    }


    @Override
    public PostFeedResponse fetchFeed(User user, int page, int limit, String cursor) {
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

                HashMap<Long, VoteType> voteMap = postMapper.getVoteTypeHashMap(user,postIds);
                HashMap<Long, Long> voteCountMap = postMapper.getVoteCountHashMap(postIds);

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

            HashMap<Long,VoteType> voteTypeHashMap = postMapper.getVoteTypeHashMap(user,postIds);
            HashMap<Long, Long> voteCountMap = postMapper.getVoteCountHashMap(postIds);

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
            HashMap<Long,VoteType> voteTypeHashMap = postMapper.getVoteTypeHashMap(user,postIds);
            HashMap<Long, Long> voteCountMap = postMapper.getVoteCountHashMap(postIds);

            List<PostResponse> postResponses = newList.stream()
                    .map(post -> postMapper.mapToPostResponse(post,voteTypeHashMap,voteCountMap))
                    .toList();
            Instant newCursor = postResponses.isEmpty() ? null : postResponses.get(postResponses.size() - 1).createdAt();
            boolean hasMore = newList.size() == limit;
            return new PostFeedResponse(postResponses, newCursor != null ? newCursor.toString() : null, hasMore);
        }
    }
}
