package com.jatin.forum.service;

import com.jatin.forum.dto.CommentResponse;
import com.jatin.forum.dto.VoteRequest;
import com.jatin.forum.entity.Comment;
import com.jatin.forum.entity.CommentVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.CommentVoteRepo;
import com.jatin.forum.repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CommentVoteService {

    private  final CommentRepo commentRepo;
    private final CommentVoteRepo commentVoteRepo;
    private final UserRepo userRepo;

    public CommentVoteService(CommentRepo commentRepo, CommentVoteRepo commentVoteRepo, UserRepo userRepo) {
        this.commentRepo = commentRepo;
        this.commentVoteRepo = commentVoteRepo;
        this.userRepo = userRepo;
    }

    public CommentResponse voteOnComment(long commentId, VoteRequest voteRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("[SERVICE] User {} voting {} on commentId: {}", email, voteRequest.voteType(), commentId);
        User user = userRepo.findByEmail(email);
        Optional<Comment> comment = commentRepo.findById(commentId);
        if(comment.isEmpty()) {
            log.warn("[SERVICE] Comment ID {} not found for voting", commentId);
            throw new RuntimeException("Comment not found");
        }
        Optional<CommentVote> commentVote = commentVoteRepo.findByUserAndComment(user, comment.get());
        if (commentVote.isEmpty()) {
            log.info("[SERVICE] No existing vote found. Saving new {} vote for comment ID: {}", voteRequest.voteType(), commentId);
            CommentVote commentVote1 = new CommentVote(user, comment.get(), voteRequest.voteType());
            commentVoteRepo.save(commentVote1);
        } else if (voteRequest.voteType().equals(commentVote.get().getVoteType())) {
            log.info("[SERVICE] User is repeating same vote type. Toggling/deleting vote from comment ID: {}", commentId);
            commentVoteRepo.delete(commentVote.get());
        } else if (!voteRequest.voteType().equals(commentVote.get().getVoteType())) {
            log.info("[SERVICE] User is changing vote type from {} to {}. Updating vote for comment ID: {}", 
                     commentVote.get().getVoteType(), voteRequest.voteType(), commentId);
            commentVote.get().setVoteType(voteRequest.voteType());
            commentVoteRepo.save(commentVote.get());
        }
        return maptoCommentResponse(comment.get());

        }

    public CommentResponse maptoCommentResponse(Comment comment) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        long upvotes = commentVoteRepo.countByCommentAndVoteType(comment, VoteType.upvote);
        long downvotes = commentVoteRepo.countByCommentAndVoteType(comment, VoteType.downvote);

        long voteCount = upvotes-downvotes;

        Optional<CommentVote> commentVote = commentVoteRepo.findByUserAndComment(user,comment);

        Long parentCommentId = null;
        if(comment.getParentComment() != null){
            parentCommentId = comment.getParentComment().getId();
        }
        VoteType voteType = commentVote.map(CommentVote::getVoteType).orElse(null);
        CommentResponse commentResponse = new CommentResponse(comment.getUser().getUsername(),comment.getId(),comment.getContent(),comment.getCreatedAt(),parentCommentId,voteCount,voteType);
        return commentResponse;
    }

    }

