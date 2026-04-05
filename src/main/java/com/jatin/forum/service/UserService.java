package com.jatin.forum.service;

import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.UpdateUsernameRequest;
import com.jatin.forum.dto.UserResponse;
import com.jatin.forum.entity.Comment;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import com.jatin.forum.exception.UserAlreadyExistsException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    public com.jatin.forum.dto.UserProfileResponse findById(Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return buildUserProfileResponse(user);
    }

    public com.jatin.forum.dto.UserProfileResponse findByUsernameProfile(String username) {
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return buildUserProfileResponse(user);
    }

    private com.jatin.forum.dto.UserProfileResponse buildUserProfileResponse(User user) {
        List<Post> posts = postRepo.getPostByUserId(user.getId());
        long postCount = posts.size();
        long commentCount = 0;
        long karma = 0;
        for (Post post : posts) {
            commentCount += commentRepo.countByPostId(post.getId());
            karma += postVoteRepo.countByPostAndVoteType(post, com.jatin.forum.entity.VoteType.upvote);
            karma -= postVoteRepo.countByPostAndVoteType(post, com.jatin.forum.entity.VoteType.downvote);
        }
        return new com.jatin.forum.dto.UserProfileResponse(user.getUsername(), user.getEmail(), user.getCreated(), postCount, commentCount, karma);
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

    public List<PostResponse> getPostsByUsernameSorted(String username, String sort) {
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return getPostsSorted(user.getId(), sort);
    }

    @Transactional
    public UserResponse updateUsername(Long userId, UpdateUsernameRequest request) {
        if (userRepo.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setUsername(request.username());
        userRepo.save(user);
        return new UserResponse(user.getUsername(), user.getEmail(), user.getCreated());
    }

    public List<UserResponse> getRecentUsers(Long currentUserId) {
        return userRepo.findUserByLastLoginAtBefore(Instant.now()).stream()
                .filter(user -> user.getLastLoginAt() != null)
                .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
                .sorted(Comparator.comparing(User::getLastLoginAt).reversed())
                .limit(5)
                .map(user -> new UserResponse(user.getUsername(), user.getEmail(), null))
                .toList();
    }
}

