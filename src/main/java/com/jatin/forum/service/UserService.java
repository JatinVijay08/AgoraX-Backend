package com.jatin.forum.service;

import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.UserResponse;

import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;

import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepo userRepo;
    private final PostRepo postRepo;
    private final PostVoteRepo postVoteRepo;
    private final CommentRepo commentRepo;
    public UserService(UserRepo userRepo, PostRepo postRepo, PostVoteRepo postVoteRepo, CommentRepo commentRepo) {
        this.userRepo = userRepo;
        this.postRepo = postRepo;

        this.postVoteRepo = postVoteRepo;
        this.commentRepo = commentRepo;
    }

    public UserResponse findById(Long id) {
        User user = userRepo.findById(id).orElseThrow(()->new RuntimeException("User not found"));
        return new UserResponse(user.getUsername(),user.getEmail(),user.getCreated());
    }


    public List<PostResponse> getPosts(Long id) {
        return postRepo.getPostByUserId(id).stream().map(post -> mapToPostResponse(post,id)).toList();
    }

    public PostResponse mapToPostResponse(Post post,Long id) {
        User user = userRepo.findById(id).orElseThrow(()->new RuntimeException("User not found"));

        long upvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.upvote);
        long downvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.downvote);

        long votes = upvotes-downvotes;
        long commentCount = commentRepo.countByPostId(post.getId());
        VoteType voteType = postVoteRepo.findByUserAndPost(user,post).map(PostVote::getVoteType).orElse(null);
        User user1 = post.getUser();
        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType);

    }

    

}
