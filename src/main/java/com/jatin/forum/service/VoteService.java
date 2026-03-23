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

    public VoteService(CommentRepo commentRepo, PostVoteRepo postVoteRepo, UserRepo userRepo, PostRepo postRepo) {
        this.commentRepo = commentRepo;
        this.postVoteRepo = postVoteRepo;
        this.userRepo = userRepo;
        this.postRepo = postRepo;
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


        long upvotes = postVoteRepo.countByPostAndVoteType(post.get(), VoteType.upvote);
        long downvotes = postVoteRepo.countByPostAndVoteType(post.get(), VoteType.downvote);

        long votes = upvotes-downvotes;

        long commentCount = commentRepo.countByPostId(post.get().getId());
        VoteType voteType1 = postVoteRepo.findByUserAndPost(user,post.get()).map(PostVote::getVoteType).orElse(null);
        User user1 = post.get().getUser();
        return new PostResponse(user1.getUsername(),post.get().getId(), post.get().getTitle(), post.get().getContent(), votes, commentCount,voteType1,post.get().getCreatedAt());

}
}
