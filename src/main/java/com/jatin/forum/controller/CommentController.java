package com.jatin.forum.controller;

import com.jatin.forum.dto.CommentResponse;
import com.jatin.forum.dto.CreateCommentRequest;
import com.jatin.forum.dto.VoteRequest;
import com.jatin.forum.service.CommentService;
import com.jatin.forum.service.CommentVoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comments")
@Slf4j
@Validated
public class CommentController {

    private final CommentService commentService;
    private final CommentVoteService commentVoteService;


    public CommentController(CommentService commentService, CommentVoteService commentVoteService) {
        this.commentService = commentService;
        this.commentVoteService = commentVoteService;
    }

    @PostMapping("/post/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable("postId") Long postId, @Valid @RequestBody CreateCommentRequest createCommentRequest) {

        return commentService.CreateComment(postId, createCommentRequest);
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable("commentId") Long commentId) {

        commentService.deleteComment(commentId);

    }

    @GetMapping("/post/{postId}")
    public Page<CommentResponse> getCommentByPostId(@PathVariable("postId") Long postId,@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {

        return commentService.getCommentByPostId(postId, page, size);
    }

    @PostMapping("{commentId}/votes")
    public CommentResponse voteOnComment(@PathVariable long commentId, @Valid @RequestBody VoteRequest voteRequest) {

        return commentVoteService.voteOnComment(commentId, voteRequest);
    }
}
