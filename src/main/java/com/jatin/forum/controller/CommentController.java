package com.jatin.forum.controller;

import com.jatin.forum.dto.CommentResponse;
import com.jatin.forum.dto.CreateCommentRequest;
import com.jatin.forum.dto.VoteRequest;
import com.jatin.forum.service.CommentService;
import com.jatin.forum.service.CommentVoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final CommentVoteService commentVoteService;


    public CommentController(CommentService commentService, CommentVoteService commentVoteService) {
        this.commentService = commentService;
        this.commentVoteService = commentVoteService;
    }

    @PostMapping("/post/{postId}")
    public CommentResponse addComment(@PathVariable("postId") Long postId, @RequestBody CreateCommentRequest createCommentRequest) {
        log.info("[CONTROLLER] Request to add comment on postId: {}", postId);
        CommentResponse response = commentService.CreateComment(postId, createCommentRequest);
        log.info("[CONTROLLER] Comment added successfully on postId: {}, commentId: {}", postId, response.id());
        return response;
    }

    @DeleteMapping("/{commentId}")
    public void deleteComment(@PathVariable("commentId") Long commentId) {
        log.info("[CONTROLLER] Request to delete commentId: {}", commentId);
        commentService.deleteComment(commentId);
        log.info("[CONTROLLER] CommentId: {} deleted successfully", commentId);
    }

    @GetMapping("/post/{postId}")
    public Page<CommentResponse> getCommentByPostId(@PathVariable("postId") Long postId,@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        log.info("[CONTROLLER] Request to fetch comments for postId: {} (page: {}, size: {})", postId, page, size);
        Page<CommentResponse> comments = commentService.getCommentByPostId(postId, page, size);
        log.info("[CONTROLLER] Fetched {} comments for postId: {}", comments.getNumberOfElements(), postId);
        return comments;
    }

    @PostMapping("{commentId}/votes")
    public CommentResponse voteOnComment(@PathVariable long commentId, @RequestBody VoteRequest voteRequest) {
        log.info("[CONTROLLER] Request to vote {} on commentId: {}", voteRequest.voteType(), commentId);
        CommentResponse response = commentVoteService.voteOnComment(commentId, voteRequest);
        log.info("[CONTROLLER] Vote updated for commentId: {}, new vote score/status response", commentId);
        return response;
    }
}
