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

    public CommentResponse voteOnComment(long commentId, VoteRequest voteRequest) {
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));
        Comment currentComment = commentRepo.findById(commentId).orElseThrow(() -> new RuntimeException("Comment not found"));
        
        Optional<CommentVote> commentVote = commentVoteRepo.findByUserAndComment(user, currentComment);
        if (commentVote.isEmpty()) {

            CommentVote commentVote1 = new CommentVote(user, currentComment, voteRequest.voteType());

            if(voteRequest.voteType().equals(VoteType.upvote)){
                currentComment.setUpvotes(currentComment.getUpvotes() + 1);
            }

            else if(voteRequest.voteType().equals(VoteType.downvote)){
                currentComment.setDownvotes(currentComment.getDownvotes() + 1);
            }

            commentVoteRepo.save(commentVote1);
            commentRepo.save(currentComment);

        } else if (voteRequest.voteType().equals(commentVote.get().getVoteType())) {

            if(voteRequest.voteType().equals(VoteType.upvote)){
                currentComment.setUpvotes(currentComment.getUpvotes() - 1);
            }
            else if(voteRequest.voteType().equals(VoteType.downvote)){
                currentComment.setDownvotes(currentComment.getDownvotes() - 1);
            }
            commentVoteRepo.delete(commentVote.get());
            commentRepo.save(currentComment);
        }
        else if (!voteRequest.voteType().equals(commentVote.get().getVoteType())) {

            commentVote.get().setVoteType(voteRequest.voteType());
            if(voteRequest.voteType().equals(VoteType.downvote)){
                currentComment.setDownvotes(currentComment.getDownvotes() + 1);
                currentComment.setUpvotes(currentComment.getUpvotes() - 1);
            }
            else if(voteRequest.voteType().equals(VoteType.upvote)){
                currentComment.setDownvotes(currentComment.getDownvotes() - 1);
                currentComment.setUpvotes(currentComment.getUpvotes() + 1);
            }
            commentVoteRepo.save(commentVote.get());
        }
        return maptoCommentResponse(currentComment);

        }

    public CommentResponse maptoCommentResponse(Comment comment) {
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));
        long upvotes = comment.getUpvotes();
        long downvotes = comment.getDownvotes();

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

