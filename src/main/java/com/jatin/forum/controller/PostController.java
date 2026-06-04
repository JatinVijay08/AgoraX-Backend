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
        log.info("[CONTROLLER] Fetching all posts (sort: {}, page: {}, limit: {}, cursor: {})", sort, page, limit, cursor);
        PostFeedResponse response = postService.getAllPosts(sort, page, limit, cursor);
        log.info("[CONTROLLER] Returned posts feed summary");
        return response;
    }

    @PostMapping(consumes = "multipart/form-data")
    public PostResponse createPost(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "media", required = false) MultipartFile media
    ) throws IOException {
        log.info("[CONTROLLER] Request to create post with title: {}", title);
        String mediaUrl = null;
        String mediaType = null;
        String mediaPublicId = null;

        if (media != null && !media.isEmpty()) {
            log.info("[CONTROLLER] Media file uploaded: {}, size: {} bytes. Uploading to Cloudinary...", media.getOriginalFilename(), media.getSize());
            Map result = cloudinaryService.upload(media);
            mediaUrl = (String) result.get("secure_url");
            mediaPublicId = (String) result.get("public_id");
            mediaType = (String) result.get("resource_type"); // "image" or "video"
            log.info("[CONTROLLER] Cloudinary upload finished: secure_url={}, resource_type={}", mediaUrl, mediaType);
        }

        PostResponse response = postService.createPost(title, content, mediaUrl, mediaType, mediaPublicId);
        log.info("[CONTROLLER] Post created successfully with ID: {}", response.id());
        return response;
    }

    @GetMapping("/{id}")
    public PostResponse getPostById(@PathVariable Long id) {
        log.info("[CONTROLLER] Fetching post by ID: {}", id);
        PostResponse response = postService.getPostById(id);
        log.info("[CONTROLLER] Found post: {}", response.title());
        return response;
    }

    @DeleteMapping("/{id}")
    public void deletePostById(@PathVariable Long id) {
        log.info("[CONTROLLER] Request to delete post by ID: {}", id);
        postService.deletePostById(id);
        log.info("[CONTROLLER] Post by ID: {} deleted successfully", id);
    }

    @PostMapping("/{postId}/votes")
    public PostResponse voteOnPost(@PathVariable Long postId, @RequestBody VoteRequest voteRequest) {
        log.info("[CONTROLLER] Request to vote {} on postId: {}", voteRequest.voteType(), postId);
        PostResponse response = voteService.voteOnPost(postId, voteRequest.voteType());
        log.info("[CONTROLLER] Vote applied to postId: {}, new vote status updated", postId);
        return response;
    }
}
