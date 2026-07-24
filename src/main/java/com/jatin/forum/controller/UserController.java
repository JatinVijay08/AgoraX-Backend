package com.jatin.forum.controller;

import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.UpdateUsernameRequest;
import com.jatin.forum.dto.UserResponse;
import com.jatin.forum.entity.User;
import com.jatin.forum.repository.UserRepo;
import com.jatin.forum.service.PostService;
import com.jatin.forum.service.UserService;
import com.jatin.forum.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final PostService postService;

    public UserController(UserService userService, CurrentUserService currentUserService, PostService postService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.postService = postService;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser().orElse(null);
    }

    @GetMapping()
    public com.jatin.forum.dto.UserProfileResponse getUser() {
        User user = currentUser();
        com.jatin.forum.dto.UserProfileResponse response = userService.findById(user.getId());
        return response;
    }

    @GetMapping("/profile/{username}")
    public com.jatin.forum.dto.UserProfileResponse getProfile(@PathVariable String username) {
        com.jatin.forum.dto.UserProfileResponse response = userService.findByUsernameProfile(username);
        return response;
    }

    @GetMapping("/profile/{username}/posts")
    public List<PostResponse> getProfilePosts(@PathVariable String username, @RequestParam(defaultValue = "new") String sort) {

        List<PostResponse> posts = userService.getPostsByUsernameSorted(username, sort);
        return posts;
    }

    @GetMapping("/posts")
    public List<PostResponse> getPosts(@RequestParam(defaultValue = "new") String sort) {
        List<PostResponse> posts = userService.getPostsSorted(currentUser().getId(), sort,currentUser());
        return posts;
    }

    @PatchMapping("/username")
    public UserResponse updateUsername(@Valid @RequestBody UpdateUsernameRequest request) {
        UserResponse response = userService.updateUsername(currentUser().getId(), request);
        return response;
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
        } catch (Exception ignored) {
        }
        return userService.getRecentUsers(currentUserId);
    }

}


