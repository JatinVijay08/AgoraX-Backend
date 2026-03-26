package com.jatin.exhale.service;

import com.jatin.exhale.dto.PostResponse;
import com.jatin.exhale.dto.UpdateUsernameRequest;
import com.jatin.exhale.dto.UserResponse;
import com.jatin.exhale.entity.User;
import com.jatin.exhale.repository.CommentRepo;
import com.jatin.exhale.repository.PostRepo;
import com.jatin.exhale.repository.PostVoteRepo;
import com.jatin.exhale.repository.UserRepo;
import com.jatin.exhale.exception.UserAlreadyExistsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return new UserResponse(user.getDisplayName(), user.getEmail(), user.getCreated());
    }

    public List<PostResponse> getPosts(Long id) {
        return postRepo.getPostByUserId(id).stream().map(postService::mapToPostResponse).toList();
    }

    public List<PostResponse> getPostsSorted(Long id, String sort) {
        List<PostResponse> posts = postRepo.getPostByUserId(id)
                .stream()
                .map(postService::mapToPostResponse)
                .toList();

        if ("top".equals(sort)) {
            return posts.stream()
                    .sorted(Comparator.comparingLong(PostResponse::voteCount).reversed())
                    .toList();
        }
        // default: new (already sorted by DB insertion order, but sort by createdAt to be explicit)
        return posts.stream()
                .sorted(Comparator.comparing(PostResponse::createdAt).reversed())
                .toList();
    }

    @Transactional
    public UserResponse updateUsername(Long userId, UpdateUsernameRequest request) {
        if (userRepo.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setDisplayName(request.username());
        userRepo.save(user);
        return new UserResponse(user.getDisplayName(), user.getEmail(), user.getCreated());
    }
}

