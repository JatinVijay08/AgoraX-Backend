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
    private final PostService postService;

    public UserService(UserRepo userRepo, PostRepo postRepo, PostVoteRepo postVoteRepo, CommentRepo commentRepo, PostService postService) {
        this.userRepo = userRepo;
        this.postRepo = postRepo;
        this.postVoteRepo = postVoteRepo;
        this.commentRepo = commentRepo;
        this.postService = postService;
    }

    public UserResponse findById(Long id) {
        User user = userRepo.findById(id).orElseThrow(()->new RuntimeException("User not found"));
        return new UserResponse(user.getUsername(),user.getEmail(),user.getCreated());
    }


    public List<PostResponse> getPosts(Long id) {
        return postRepo.getPostByUserId(id).stream().map(postService::mapToPostResponse).toList();
    }

    

}
