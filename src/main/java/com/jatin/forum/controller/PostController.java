package com.jatin.forum.controller;

import com.jatin.forum.dto.PostFeedResponse;
import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.VoteRequest;
import com.jatin.forum.service.CloudinaryService;
import com.jatin.forum.service.PostService;
import com.jatin.forum.service.VoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/posts")
@Slf4j
public class PostController {

    private final PostService postService;
    private final VoteService voteService;
    private final CloudinaryService cloudinaryService;

    public PostController(PostService postService, VoteService voteService, CloudinaryService cloudinaryService) {
        this.postService = postService;
        this.voteService = voteService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public PostFeedResponse getAllPosts(@RequestParam(defaultValue = "new") String sort, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int limit, @RequestParam(required = false) String cursor) {

        PostFeedResponse response = postService.getAllPosts(sort, page, limit, cursor);

        return response;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "media", required = false) MultipartFile media
    ) throws IOException {

        String mediaUrl = null;
        String mediaType = null;
        String mediaPublicId = null;

        if (media != null && !media.isEmpty()) {

            Map result = cloudinaryService.upload(media);
            mediaUrl = (String) result.get("secure_url");
            mediaPublicId = (String) result.get("public_id");
            mediaType = (String) result.get("resource_type"); // "image" or "video"
        }

        return postService.createPost(title, content, mediaUrl, mediaType, mediaPublicId);
    }

    @GetMapping("/{id}")
    public PostResponse getPostById(@PathVariable Long id) {
        PostResponse response = postService.getPostById(id);
        return response;
    }

    @DeleteMapping("/{id}")
    public void deletePostById(@PathVariable Long id) {
        postService.deletePostById(id);
    }

    @PostMapping("/{postId}/votes")
    public PostResponse voteOnPost(@PathVariable Long postId, @RequestBody VoteRequest voteRequest) {
        return voteService.voteOnPost(postId, voteRequest.voteType());
    }
}
