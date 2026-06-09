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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

           // if single vote is made in 2 minutes, count will reset
           String key = "counter:"+"votes";
           Long count =  redisTemplate.opsForValue().increment(key);

           if(count!=null && count==1 ){
               // set ttl on first increment
               redisTemplate.expire(key,2, TimeUnit.MINUTES);
           }// if multiple votes are made within that 2 minutes window while it is still active
           if(count!=null && count>=20){
               redisTemplate.delete("feed:hot");
               redisTemplate.delete("feed:trending");
               redisTemplate.opsForValue().set(key,"0",150,TimeUnit.SECONDS);
           }
         return postService.mapToPostResponse(post.get(),user);
}
}
