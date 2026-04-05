package com.jatin.forum.controller;

import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.UpdateUsernameRequest;
import com.jatin.forum.dto.UserResponse;
import com.jatin.forum.entity.User;
import com.jatin.forum.repository.UserRepo;
import com.jatin.forum.service.PostService;
import com.jatin.forum.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
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
        return userRepo.findByEmail(auth.getName());
    }

    @GetMapping()
    public com.jatin.forum.dto.UserProfileResponse getUser() {
        return userService.findById(currentUser().getId());
    }

    @GetMapping("/profile/{username}")
    public com.jatin.forum.dto.UserProfileResponse getProfile(@PathVariable String username) {
        return userService.findByUsernameProfile(username);
    }

    @GetMapping("/profile/{username}/posts")
    public List<PostResponse> getProfilePosts(@PathVariable String username, @RequestParam(defaultValue = "new") String sort) {
        return userService.getPostsByUsernameSorted(username, sort);
    }

    @GetMapping("/posts")
    public List<PostResponse> getPosts(@RequestParam(defaultValue = "new") String sort) {
        return userService.getPostsSorted(currentUser().getId(), sort);
    }

    @PatchMapping("/username")
    public UserResponse updateUsername(@Valid @RequestBody UpdateUsernameRequest request) {
        return userService.updateUsername(currentUser().getId(), request);
    }

    @DeleteMapping("/posts/{postId}")
    public void deletePost(@PathVariable Long postId) {
        postService.deletePostById(postId);
    }

    @GetMapping("/recent")
    public List<UserResponse> getUsersRecent(){
        Long currentUserId = null;
        try {
            User current = currentUser();
            if (current != null) {
                currentUserId = current.getId();
            }
        } catch (Exception e) {
            // Ignore if no current authenticated user
        }
        return userService.getRecentUsers(currentUserId);
    }

}


