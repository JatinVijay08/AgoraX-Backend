package com.jatin.forum.service;



import com.jatin.forum.dto.CachedFeed;
import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.*;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;


import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
public class VoteService {


    private  final PostVoteRepo postVoteRepo;
    private  final UserRepo userRepo;
    private  final PostRepo postRepo;
    private final PostMapper postMapper;
    private final NotificationService notificationService;
    private final FeedCacheService feedCacheService;
    private final CurrentUserService currentUserService;

    public VoteService(PostVoteRepo postVoteRepo, UserRepo userRepo, PostRepo postRepo, PostMapper postMapper, NotificationService notificationService, FeedCacheService feedCacheService, CurrentUserService currentUserService) {
        this.postVoteRepo = postVoteRepo;
        this.userRepo = userRepo;
        this.postRepo = postRepo;
        this.postMapper = postMapper;
        this.notificationService = notificationService;
        this.feedCacheService = feedCacheService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public PostResponse voteOnPost(Long postId, VoteType voteType) {
           User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));
           Post currentPost = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("post not found"));

           VoteType finalVoteType = voteType;
           Optional<PostVote> postVote = postVoteRepo.findByUserAndPost(user,currentPost);
           if(postVote.isEmpty()){
               PostVote postVote1 = new PostVote(user,currentPost,voteType);
               if(VoteType.upvote.equals(voteType)){
                   // increment upvote
                   postRepo.incrementUpvoteCount(postId);

               }
               else if(VoteType.downvote.equals(voteType)){
                   // increment downvote
                   postRepo.incrementDownvoteCount(postId);
               }
               postVoteRepo.save(postVote1);
               if(VoteType.upvote.equals(voteType)) {
                   notificationService.createNotification(currentPost, user, NotificationType.POST_LIKE);
               }
           }
           else if(postVote.get().getVoteType().equals(voteType)){
               if(VoteType.upvote.equals(voteType)){
                   // decrement upvote
                   postRepo.decrementUpvoteCount(postId);

               }
               else if(VoteType.downvote.equals(voteType)){
                   // decrement downvote
                   postRepo.decrementDownvoteCount(postId);

               }
               postVoteRepo.delete(postVote.get());
               finalVoteType = null;
           }
           else if(!postVote.get().getVoteType().equals(voteType)){
               postVote.get().setVoteType(voteType);
               if(VoteType.upvote.equals(voteType)){
                   // increment upvote , decrement downvote
                   postRepo.incrementUpvoteCount(postId);
                   postRepo.decrementDownvoteCount(postId);

               }
               else if(VoteType.downvote.equals(voteType)){
                   // increment downvote,decrement upvote
                   postRepo.incrementDownvoteCount(postId);
                   postRepo.decrementUpvoteCount(postId);

               }
               postVoteRepo.save(postVote.get());

               if(VoteType.upvote.equals(voteType)) {
                   notificationService.createNotification(currentPost, user, NotificationType.POST_LIKE);
               }
           }

           // if a votes is made -> INCREASE ACTIVITY ON HOT AND TRENDING FEED ACTIVITY KEYS
           long hotCount = feedCacheService.incrementActivity(FeedCacheService.TYPE_HOT);
           long trendingCount = feedCacheService.incrementActivity(FeedCacheService.TYPE_TRENDING);
           if(feedCacheService.shouldEvict(FeedCacheService.TYPE_HOT, hotCount)){
               feedCacheService.evictFeed(FeedCacheService.TYPE_HOT, 10);
               feedCacheService.resetActivity(FeedCacheService.TYPE_HOT);
           }
           if(feedCacheService.shouldEvict(FeedCacheService.TYPE_TRENDING, trendingCount)){
               feedCacheService.resetActivity(FeedCacheService.TYPE_TRENDING);
               feedCacheService.evictFeed(FeedCacheService.TYPE_TRENDING, 10);
           }

           // new vote type returned
         HashMap<Long,VoteType> map = new HashMap<>();
         if (finalVoteType != null) {
             map.put(postId, finalVoteType);
         }
         
         List<Object[]> results = postVoteRepo.getAggregateVotesForPosts(List.of(postId));
         HashMap<Long, Long> voteCountMap = new HashMap<>();
         for (Object[] result : results) {
             voteCountMap.put((Long) result[0], ((Number) result[1]).longValue());
         }
         
         return postMapper.mapToPostResponse(currentPost, map, voteCountMap); // fetch from db
}
}
