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
import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CommentVoteService {

    private  final CommentRepo commentRepo;
    private final CommentVoteRepo commentVoteRepo;
    private final UserRepo userRepo;
    private final CurrentUserService currentUserService;

    public CommentVoteService(CommentRepo commentRepo, CommentVoteRepo commentVoteRepo, UserRepo userRepo, CurrentUserService currentUserService) {
        this.commentRepo = commentRepo;
        this.commentVoteRepo = commentVoteRepo;
        this.userRepo = userRepo;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public CommentResponse voteOnComment(long commentId, VoteRequest voteRequest) {
        VoteType finalVoteType = voteRequest.voteType();
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));
        Comment currentComment = commentRepo.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));
        
        Optional<CommentVote> commentVote = commentVoteRepo.findByUserAndComment(user, currentComment);
        if (commentVote.isEmpty()) {

            CommentVote commentVote1 = new CommentVote(user, currentComment, voteRequest.voteType());

            if(voteRequest.voteType().equals(VoteType.upvote)){
                commentRepo.incrementUpvoteCount(commentId);
            }

            else if(voteRequest.voteType().equals(VoteType.downvote)){
                commentRepo.incrementDownvoteCount(commentId);
            }

            commentVoteRepo.save(commentVote1);
            commentRepo.save(currentComment);

        } else if (voteRequest.voteType().equals(commentVote.get().getVoteType())) {

            if(voteRequest.voteType().equals(VoteType.upvote)){
                commentRepo.decrementUpvoteCount(commentId);
            }
            else if(voteRequest.voteType().equals(VoteType.downvote)){
                commentRepo.decrementDownvoteCount(commentId);
            }
            commentVoteRepo.delete(commentVote.get());
            commentRepo.save(currentComment);
            finalVoteType = null;
        }
        else if (!voteRequest.voteType().equals(commentVote.get().getVoteType())) {

            commentVote.get().setVoteType(voteRequest.voteType());
            if(voteRequest.voteType().equals(VoteType.downvote)){
                commentRepo.incrementDownvoteCount(commentId);
                commentRepo.decrementUpvoteCount(commentId);
            }
            else if(voteRequest.voteType().equals(VoteType.upvote)){
                commentRepo.incrementUpvoteCount(commentId);
                commentRepo.decrementDownvoteCount(commentId);
            }
            commentVoteRepo.save(commentVote.get());
        }
        return maptoCommentResponse(currentComment, finalVoteType);

        }

    public CommentResponse maptoCommentResponse(Comment comment, VoteType finalVoteType) {
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));
        long upvotes = comment.getUpvotes();
        long downvotes = comment.getDownvotes();

        long voteCount = upvotes-downvotes;


        Long parentCommentId = null;
        if(comment.getParentComment() != null){
            parentCommentId = comment.getParentComment().getId();
        }
        CommentResponse commentResponse = new CommentResponse(comment.getUser().getUsername(),comment.getId(),comment.getContent(),comment.getCreatedAt(),parentCommentId,voteCount,finalVoteType);
        return commentResponse;
    }

    }

