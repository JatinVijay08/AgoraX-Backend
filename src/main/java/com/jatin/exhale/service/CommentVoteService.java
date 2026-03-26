package com.jatin.exhale.service;

import com.jatin.exhale.dto.CommentResponse;
import com.jatin.exhale.dto.VoteRequest;
import com.jatin.exhale.entity.Comment;
import com.jatin.exhale.entity.CommentVote;
import com.jatin.exhale.entity.User;
import com.jatin.exhale.entity.VoteType;
import com.jatin.exhale.repository.CommentRepo;
import com.jatin.exhale.repository.CommentVoteRepo;
import com.jatin.exhale.repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
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
        User user = userRepo.findByEmail(email);
        Optional<Comment> comment = commentRepo.findById(commentId);
        Optional<CommentVote> commentVote = commentVoteRepo.findByUserAndComment(user, comment.get());
        if(comment.isPresent()) {
            if (commentVote.isEmpty()) {
                CommentVote commentVote1 = new CommentVote(user, comment.get(), voteRequest.voteType());
                commentVoteRepo.save(commentVote1);
            } else if (voteRequest.voteType().equals(commentVote.get().getVoteType())) {
                commentVoteRepo.delete(commentVote.get());
            } else if (!voteRequest.voteType().equals(commentVote.get().getVoteType())) {
                commentVote.get().setVoteType(voteRequest.voteType());
                commentVoteRepo.save(commentVote.get());
            }
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
        CommentResponse commentResponse = new CommentResponse(comment.getUser().getDisplayName(),comment.getId(),comment.getContent(),comment.getCreatedAt(),parentCommentId,voteCount,voteType);
        return commentResponse;
    }

    }

