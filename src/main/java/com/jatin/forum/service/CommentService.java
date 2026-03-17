package com.jatin.forum.service;

import com.jatin.forum.dto.CommentResponse;
import com.jatin.forum.dto.CreateCommentRequest;
import com.jatin.forum.entity.*;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.CommentVoteRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.UserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CommentService {

    private final PostRepo postRepo;
    private final CommentRepo commentRepo;
    private final CommentVoteRepo commentVoteRepo;
    private final UserRepo userRepo;
    public CommentService(PostRepo postRepo, CommentRepo commentRepo, CommentVoteRepo commentVoteRepo, UserRepo userRepo) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.commentVoteRepo = commentVoteRepo;
        this.userRepo = userRepo;
    }

    public CommentResponse CreateComment(Long postID, CreateCommentRequest createCommentRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Optional<Post> post = postRepo.findById(postID);
        if (post.isEmpty()) {
            throw new RuntimeException("Post not found");
        }
        Comment comment = new Comment(createCommentRequest.content(), user, post.get());

        if(createCommentRequest.parentId() != null) {
            Comment parent = commentRepo.findById(createCommentRequest.parentId()).orElseThrow(()->new RuntimeException("Parent comment not found"));
            comment.setParentComment(parent);
        }
        commentRepo.save(comment);
        return maptoCommentResponse(comment);
    }

    public void deleteComment(Long commentID) {
        Optional<Comment> comment = commentRepo.findById(commentID);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if (comment.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }
        if(!comment.get().getUser().getEmail().equals(user.getEmail())) {
        throw new RuntimeException("You are not allowed to delete this comment");
        }
        commentRepo.delete(comment.get());
    }

    public Page<CommentResponse> getCommentByPostId(Long postId, int page,int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt"));
        Page<Comment> comments =  commentRepo.findByPostId(postId, pageable);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email;
        if(authentication!=null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            email = authentication.getName();
        } else {
            email = null;
        }
        return comments.map(comment->{
            long upvotes = commentVoteRepo.countByCommentAndVoteType(comment, VoteType.upvote);
            long downvotes = commentVoteRepo.countByCommentAndVoteType(comment, VoteType.downvote);

            long voteCount = upvotes-downvotes;


            VoteType voteType=null;

            if(email!=null) {
                User user = userRepo.findByEmail(email);
                Optional<CommentVote> commentVote = commentVoteRepo.findByUserAndComment(user,comment);
                voteType = commentVote.map(CommentVote::getVoteType).orElse(null);
            }


            Long parentCommentId = null;

            if(comment.getParentComment() != null){
                parentCommentId = comment.getParentComment().getId();
            }


            CommentResponse commentResponse = new CommentResponse(comment.getUser().getUsername(),comment.getId(),comment.getContent(),comment.getCreatedAt(),parentCommentId,voteCount,voteType);
            return commentResponse;
        });

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
