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

import com.jatin.forum.strategy.FeedStrategy;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.jatin.forum.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;


import java.util.*;


@Service
@Slf4j
public class PostService {
    private final PostVoteRepo postVoteRepo;
    private final CommentRepo commentRepo;
    private final PostRepo postRepo;
    private final FeedCacheService feedCacheService;
    private final CurrentUserService currentUserService;
    private final PostMapper postMapper;
    private final Map<String, FeedStrategy>  feedStrategyMap;


    public PostService(PostRepo postRepo, PostVoteRepo postVoteRepo, CommentRepo commentRepo, FeedCacheService feedCacheService, CurrentUserService currentUserService, PostMapper postMapper, Map<String, FeedStrategy> feedStrategyMap) {
        this.postRepo = postRepo;
        this.postVoteRepo = postVoteRepo;
        this.commentRepo = commentRepo;
        this.feedCacheService = feedCacheService;
        this.currentUserService = currentUserService;
        this.postMapper = postMapper;
        this.feedStrategyMap = feedStrategyMap;
    }



    public PostFeedResponse getAllPosts(String sort, int page,int  limit, String cursor){
             User user = currentUserService.getCurrentUser().orElse(null);
             FeedStrategy feedStrategy = feedStrategyMap.get(sort);
             if(feedStrategy==null){
                 return new PostFeedResponse(List.of(), null, false);
             }
             return feedStrategy.fetchFeed(user,page,limit,cursor);
    }







    public PostResponse createPost(String title, String content, String mediaUrl, String mediaType, String mediaPublicId){
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
            return new ResourceNotFoundException("post not found");
        });
        HashMap<Long,VoteType> voteTypeHashMap = new HashMap<>();
        HashMap<Long, Long> voteCountMap = postMapper.getVoteCountHashMap(Collections.singletonList(id));
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
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Post post = postRepo.findById(postId).orElseThrow(()-> {
            return new ResourceNotFoundException("post not found");
        });
        if(!post.getUser().getId().equals(user.getId())){
           throw new AccessDeniedException("Not allowed to delete post");
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
