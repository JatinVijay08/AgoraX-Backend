package com.jatin.forum.service;



import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class VoteService {


    private  final PostVoteRepo postVoteRepo;
    private  final UserRepo userRepo;
    private  final PostRepo postRepo;
    private final PostService postService;
    private final RedisTemplate<String, String> redisTemplate;

    public VoteService(PostVoteRepo postVoteRepo, UserRepo userRepo, PostRepo postRepo, PostService postService, RedisTemplate<String, String> redisTemplate) {
        this.postVoteRepo = postVoteRepo;
        this.userRepo = userRepo;
        this.postRepo = postRepo;
        this.postService = postService;
        this.redisTemplate = redisTemplate;
    }

    public PostResponse voteOnPost(Long postId, VoteType voteType) {
           Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
           String email = authentication.getName();
           User user = userRepo.findByEmail(email);
           Optional<Post> post = postRepo.findById(postId);
           if(post.isEmpty()){
               throw new RuntimeException("post not found");
           }

           Optional<PostVote> postVote = postVoteRepo.findByUserAndPost(user,post.get());
           if(postVote.isEmpty()){
               PostVote postVote1 = new PostVote(user,post.get(),voteType);
               if(VoteType.upvote.equals(voteType)){
                   post.get().setUpvotesCount(post.get().getUpvotesCount()+1);
               }
               else if(VoteType.downvote.equals(voteType)){
                   post.get().setDownvotesCount(post.get().getDownvotesCount()+1);
               }
               postVoteRepo.save(postVote1);
               postRepo.save(post.get());
           }
           else if(postVote.get().getVoteType().equals(voteType)){
               if(VoteType.upvote.equals(voteType)){
                   post.get().setUpvotesCount(post.get().getUpvotesCount()-1);
               }
               else if(VoteType.downvote.equals(voteType)){
                   post.get().setDownvotesCount(post.get().getDownvotesCount()-1);
               }
               postVoteRepo.delete(postVote.get());
               postRepo.save(post.get());
           }
           else if(!postVote.get().getVoteType().equals(voteType)){
               postVote.get().setVoteType(voteType);
               if(VoteType.upvote.equals(voteType)){
                   post.get().setUpvotesCount(post.get().getUpvotesCount()+1);
                   post.get().setDownvotesCount(post.get().getDownvotesCount()-1);
               }
               else if(VoteType.downvote.equals(voteType)){
                   post.get().setDownvotesCount(post.get().getDownvotesCount()+1);
                   post.get().setUpvotesCount(post.get().getUpvotesCount()-1);
               }
               postVoteRepo.save(postVote.get());
           }

           // if a votes is made -> delete cache , load from db , only that post is updated
         Set<String> keys = redisTemplate.keys("feed:hot:*");
         Set<String> keys2 = redisTemplate.keys("feed:trending:*");
           if(keys!=null && !keys.isEmpty()){
               redisTemplate.delete(keys);
           }
           if(keys2!=null && !keys2.isEmpty()){
               redisTemplate.delete(keys2);
           }

           // new vote type returned
        HashMap<Long,VoteType> map = new HashMap<>();
           map.put(postId,voteType);
         return postService.mapToPostResponse(post.get(),user,map); // fetch from db
}
}
