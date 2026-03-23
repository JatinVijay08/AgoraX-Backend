package com.jatin.forum.controller;

import com.jatin.forum.dto.CreatePostRequest;
import com.jatin.forum.dto.PostFeedResponse;
import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.VoteRequest;
import com.jatin.forum.service.PostService;
import com.jatin.forum.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final VoteService voteService;

    public PostController(PostService postService, VoteService voteService) {
        this.postService = postService;
        this.voteService = voteService;
    }

    @GetMapping
    public PostFeedResponse getAllPosts(@RequestParam(defaultValue = "new") String sort, @RequestParam(defaultValue="10") int page, @RequestParam(required=false) String cursor) {
        return postService.getAllPosts(sort,page,cursor);
    }

    @PostMapping
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest createPostRequest) {
        return postService.createPost(createPostRequest) ;
    }

    @GetMapping("/{id}")
    public PostResponse getPostById(@PathVariable Long id){
        return postService.getPostById(id);
    }

    @DeleteMapping("/{id}")
    public void deletePostById(@PathVariable Long id){
        postService.deletePostById(id);
    }

    @PostMapping("/{postId}/votes")
    public PostResponse voteOnPost(@PathVariable Long postId, @RequestBody VoteRequest  voteRequest){
        return voteService.voteOnPost(postId,voteRequest.voteType());
    }


}
