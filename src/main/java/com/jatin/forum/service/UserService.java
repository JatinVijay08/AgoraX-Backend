package com.jatin.forum.service;

import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.UpdateUsernameRequest;
import com.jatin.forum.dto.UserResponse;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.User;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import com.jatin.forum.exception.UserAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class UserService {
    private final UserRepo userRepo;
    private final PostRepo postRepo;
    private final CommentRepo commentRepo;
    private final PostService postService;

    public UserService(UserRepo userRepo, PostRepo postRepo, PostVoteRepo postVoteRepo, CommentRepo commentRepo, PostService postService) {
        this.userRepo = userRepo;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.postService = postService;
    }


    public com.jatin.forum.dto.UserProfileResponse findById(Long id) {
        log.info("[SERVICE] Fetching user profile by ID: {}", id);
        User user = userRepo.findById(id).orElseThrow(() -> {
            log.warn("[SERVICE] User ID {} not found", id);
            return new RuntimeException("User not found");
        });
        return buildUserProfileResponse(user);
    }

    public com.jatin.forum.dto.UserProfileResponse findByUsernameProfile(String username) {
        log.info("[SERVICE] Fetching user profile by username: {}", username);
        User user = userRepo.findByUsername(username).orElseThrow(() -> {
            log.warn("[SERVICE] Username {} not found", username);
            return new RuntimeException("User not found");
        });
        return buildUserProfileResponse(user);
    }

    private com.jatin.forum.dto.UserProfileResponse buildUserProfileResponse(User user) {
        log.info("[SERVICE] Computing profile stats (posts, comments, karma) for: {}", user.getUsername());
        List<Post> posts = postRepo.getPostByUserId(user.getId());
        long postCount = posts.size();
        long commentCount = 0;
        long karma = 0;
        for (Post post : posts) {
            commentCount += post.getCommentCount();
            karma += post.getUpvotesCount();
            karma -= post.getDownvotesCount();
        }
        log.info("[SERVICE] Profile stats computed: postCount={}, commentCount={}, karma={}", postCount, commentCount, karma);
        return new com.jatin.forum.dto.UserProfileResponse(user.getUsername(), user.getEmail(), user.getCreated(), postCount, commentCount, karma);
    }


    public List<PostResponse> getPostsSorted(Long id, String sort,User user) {
        log.info("[SERVICE] Fetching sorted posts for User ID: {} (sort: {})", id, sort);
        List<PostResponse> posts = postRepo.getPostByUserId(id)
                .stream()
                .map(post -> postService.mapToPostResponse(post,user))
                .toList();

        if ("top".equals(sort)) {
            log.info("[SERVICE] Sorting posts by top (votes)");
            return posts.stream()
                    .sorted(Comparator.comparingLong(PostResponse::voteCount).reversed())
                    .toList();
        }
        // default: new (already sorted by DB insertion order, but sort by createdAt to be explicit)
        log.info("[SERVICE] Sorting posts by new (creation date)");
        return posts.stream()
                .sorted(Comparator.comparing(PostResponse::createdAt).reversed())
                .toList();
    }

    public List<PostResponse> getPostsByUsernameSorted(String username, String sort) {
        log.info("[SERVICE] Fetching posts for username: {} sorted by: {}", username, sort);
        User user = userRepo.findByUsername(username).orElseThrow(() -> {
            log.warn("[SERVICE] Username {} not found during sorted posts fetch", username);
            return new RuntimeException("User not found");
        });
        return getPostsSorted(user.getId(), sort,user);
    }

    @Transactional
    public UserResponse updateUsername(Long userId, UpdateUsernameRequest request) {
        log.info("[SERVICE] Attempting to update username for user ID: {} to {}", userId, request.username());
        if (userRepo.existsByUsername(request.username())) {
            log.warn("[SERVICE] Username update failed: '{}' is already taken", request.username());
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }
        User user = userRepo.findById(userId).orElseThrow(() -> {
            log.error("[SERVICE] User ID {} not found during username update", userId);
            return new RuntimeException("User not found");
        });
        user.setUsername(request.username());
        userRepo.save(user);
        log.info("[SERVICE] Username updated successfully in database for User ID {}", userId);
        return new UserResponse(user.getUsername(), user.getEmail(), user.getCreated());
    }

    public List<UserResponse> getRecentUsers(Long currentUserId) {
        log.info("[SERVICE] Fetching active recent users list");
        return userRepo.findUserByLastLoginAtBefore(Instant.now()).stream()
                .filter(user -> user.getLastLoginAt() != null)
                .filter(user -> !user.getId().equals(currentUserId))
                .sorted(Comparator.comparing(User::getLastLoginAt).reversed())
                .limit(5)
                .map(user -> new UserResponse(user.getUsername(), user.getEmail(), null))
                .toList();
    }
}

