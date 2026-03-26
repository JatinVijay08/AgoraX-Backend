package com.jatin.exhale.controller;

import com.jatin.exhale.dto.PostResponse;
import com.jatin.exhale.dto.UpdateUsernameRequest;
import com.jatin.exhale.dto.UserResponse;
import com.jatin.exhale.entity.User;
import com.jatin.exhale.repository.UserRepo;
import com.jatin.exhale.service.PostService;
import com.jatin.exhale.service.UserService;
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
    public UserResponse getUser() {
        return userService.findById(currentUser().getId());
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
}

