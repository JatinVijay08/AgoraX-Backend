package com.jatin.forum.controller;

import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.UpdateUsernameRequest;
import com.jatin.forum.dto.UserResponse;
import com.jatin.forum.entity.User;
import com.jatin.forum.repository.UserRepo;
import com.jatin.forum.service.PostService;
import com.jatin.forum.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final UserService userService;
    private final UserRepo userRepo;
    private final PostService postService;

    public UserController(UserService userService, UserRepo userRepo, PostService postService) {
        this.userService = userService;
        this.userRepo = userRepo;
        this.postService = postService;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("[CONTROLLER] Retrieving current authenticated user from SecurityContext: {}", auth != null ? auth.getName() : "null");
        User user = userRepo.findByEmail(auth.getName());
        if (user != null) {
            log.info("[CONTROLLER] Authenticated User details: email={}, id={}", user.getEmail(), user.getId());
        }
        return user;
    }

    @GetMapping()
    public com.jatin.forum.dto.UserProfileResponse getUser() {
        log.info("[CONTROLLER] Request to fetch profile of the currently logged-in user");
        User user = currentUser();
        com.jatin.forum.dto.UserProfileResponse response = userService.findById(user.getId());
        log.info("[CONTROLLER] Found profile details for {}", user.getEmail());
        return response;
    }

    @GetMapping("/profile/{username}")
    public com.jatin.forum.dto.UserProfileResponse getProfile(@PathVariable String username) {
        log.info("[CONTROLLER] Request to fetch profile for username: {}", username);
        com.jatin.forum.dto.UserProfileResponse response = userService.findByUsernameProfile(username);
        log.info("[CONTROLLER] Found profile details for username: {}", username);
        return response;
    }

    @GetMapping("/profile/{username}/posts")
    public List<PostResponse> getProfilePosts(@PathVariable String username, @RequestParam(defaultValue = "new") String sort) {
        log.info("[CONTROLLER] Request to fetch profile posts for username: {} (sort: {})", username, sort);
        List<PostResponse> posts = userService.getPostsByUsernameSorted(username, sort);
        log.info("[CONTROLLER] Fetched {} posts for username: {}", posts.size(), username);
        return posts;
    }

    @GetMapping("/posts")
    public List<PostResponse> getPosts(@RequestParam(defaultValue = "new") String sort) {
        log.info("[CONTROLLER] Request to fetch posts of current user (sort: {})", sort);
        List<PostResponse> posts = userService.getPostsSorted(currentUser().getId(), sort);
        log.info("[CONTROLLER] Fetched {} posts for current user", posts.size());
        return posts;
    }

    @PatchMapping("/username")
    public UserResponse updateUsername(@Valid @RequestBody UpdateUsernameRequest request) {
        log.info("[CONTROLLER] Request to update username to: {}", request.username());
        UserResponse response = userService.updateUsername(currentUser().getId(), request);
        log.info("[CONTROLLER] Username updated successfully");
        return response;
    }

    @DeleteMapping("/posts/{postId}")
    public void deletePost(@PathVariable Long postId) {
        log.info("[CONTROLLER] Request to delete post ID: {}", postId);
        postService.deletePostById(postId);
        log.info("[CONTROLLER] Post ID: {} deleted", postId);
    }

    @GetMapping("/recent")
    public List<UserResponse> getUsersRecent(){
        log.info("[CONTROLLER] Request to fetch recent active users");
        Long currentUserId = null;
        try {
            User current = currentUser();
            if (current != null) {
                currentUserId = current.getId();
            }
        } catch (Exception e) {
            log.warn("[CONTROLLER] No authenticated user detected during recent users fetch: {}", e.getMessage());
        }
        List<UserResponse> recentUsers = userService.getRecentUsers(currentUserId);
        log.info("[CONTROLLER] Fetched {} recent users", recentUsers.size());
        return recentUsers;
    }

}


