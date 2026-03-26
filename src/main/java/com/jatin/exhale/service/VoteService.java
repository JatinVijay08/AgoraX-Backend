package com.jatin.exhale.service;



import com.jatin.exhale.dto.PostResponse;
import com.jatin.exhale.entity.Post;
import com.jatin.exhale.entity.PostVote;
import com.jatin.exhale.entity.User;
import com.jatin.exhale.entity.VoteType;
import com.jatin.exhale.repository.CommentRepo;
import com.jatin.exhale.repository.PostRepo;
import com.jatin.exhale.repository.PostVoteRepo;
import com.jatin.exhale.repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class VoteService {

    private final CommentRepo commentRepo;
    private  final PostVoteRepo postVoteRepo;
    private  final UserRepo userRepo;
    private  final PostRepo postRepo;
    private final PostService postService;

    public VoteService(CommentRepo commentRepo, PostVoteRepo postVoteRepo, UserRepo userRepo, PostRepo postRepo, PostService postService) {
        this.commentRepo = commentRepo;
        this.postVoteRepo = postVoteRepo;
        this.userRepo = userRepo;
        this.postRepo = postRepo;
        this.postService = postService;
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
               postVoteRepo.save(postVote1);
           }
           else if(postVote.get().getVoteType().equals(voteType)){
               postVoteRepo.delete(postVote.get());
           }
           else if(!postVote.get().getVoteType().equals(voteType)){
               postVote.get().setVoteType(voteType);
               postVoteRepo.save(postVote.get());
           }


        return postService.mapToPostResponse(post.get());

}
}
