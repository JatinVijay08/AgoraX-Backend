package com.jatin.forum.service;



import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
           log.info("[SERVICE] User {} voting {} on postId: {}", email, voteType, postId);
           User user = userRepo.findByEmail(email);
           Optional<Post> post = postRepo.findById(postId);
           if(post.isEmpty()){
               log.warn("[SERVICE] Post ID {} not found for voting", postId);
               throw new RuntimeException("post not found");
           }

           Optional<PostVote> postVote = postVoteRepo.findByUserAndPost(user,post.get());
           if(postVote.isEmpty()){
               log.info("[SERVICE] No existing post vote found. Saving new {} vote for post ID: {}", voteType, postId);
               PostVote postVote1 = new PostVote(user,post.get(),voteType);
               postVoteRepo.save(postVote1);
           }
           else if(postVote.get().getVoteType().equals(voteType)){
               log.info("[SERVICE] User is repeating same vote type. Toggling/deleting vote from post ID: {}", postId);
               postVoteRepo.delete(postVote.get());
           }
           else if(!postVote.get().getVoteType().equals(voteType)){
               log.info("[SERVICE] User is changing vote type from {} to {}. Updating vote for post ID: {}", 
                        postVote.get().getVoteType(), voteType, postId);
               postVote.get().setVoteType(voteType);
               postVoteRepo.save(postVote.get());
           }

           // if single vote is made in 2 minutes, count will reset
           String key = "counter:"+"votes";
           Long count =  redisTemplate.opsForValue().increment(key);
           log.info("[SERVICE] Redis vote counter incremented. Key: {}, Current Count: {}", key, count);

           if(count!=null && count==1 ){
               // set ttl on first increment
               redisTemplate.expire(key,2, TimeUnit.MINUTES);
               log.info("[SERVICE] Set 2-minute expiration on Redis key: {}", key);
           }// if multiple votes are made within that 2 minutes window while it is still active
           if(count!=null && count>=20){
               log.info("[SERVICE] Vote threshold reached (count >= 20). Invalidating feed caches from Redis...");
               redisTemplate.delete("feed:hot");
               redisTemplate.delete("feed:trending");
               redisTemplate.opsForValue().set(key,"0",150,TimeUnit.SECONDS);
               log.info("[SERVICE] Caches cleared and counter reset.");
           }
         return postService.mapToPostResponse(post.get());
}
}
