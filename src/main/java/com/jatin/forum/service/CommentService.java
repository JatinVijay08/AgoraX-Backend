package com.jatin.forum.service;

import com.jatin.forum.dto.CommentResponse;
import com.jatin.forum.dto.CreateCommentRequest;
import com.jatin.forum.entity.*;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.CommentVoteRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
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
        log.info("[SERVICE] Creating comment on postID: {} by user: {}", postID, email);
        User user = userRepo.findByEmail(email);
        if (user == null) {
            log.error("[SERVICE] Comment creation failed: User not found with email: {}", email);
            throw new RuntimeException("User not found");
        }

        Optional<Post> post = postRepo.findById(postID);
        if (post.isEmpty()) {
            log.warn("[SERVICE] Comment creation failed: Post not found with ID: {}", postID);
            throw new RuntimeException("Post not found");
        }
        Comment comment = new Comment(createCommentRequest.content(), user, post.get());

        if(createCommentRequest.parentId() != null) {
            log.info("[SERVICE] Finding parent comment with ID: {}", createCommentRequest.parentId());
            Comment parent = commentRepo.findById(createCommentRequest.parentId()).orElseThrow(()-> {
                log.warn("[SERVICE] Parent comment ID {} not found", createCommentRequest.parentId());
                return new RuntimeException("Parent comment not found");
            });
            comment.setParentComment(parent);
        }
        commentRepo.save(comment);

        long newCommentCount = post.get().getCommentCount();
        post.get().setCommentCount(newCommentCount+1);
        postRepo.save(post.get());
        log.info("[SERVICE] Comment successfully saved to DB with ID: {}", comment.getId());
        return maptoCommentResponse(comment);
    }

    public void deleteComment(Long commentID) {
        log.info("[SERVICE] Deleting comment ID: {}", commentID);
        Optional<Comment> comment = commentRepo.findById(commentID);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if (comment.isEmpty()) {
            log.warn("[SERVICE] Comment deletion failed: Comment ID {} not found", commentID);
            throw new RuntimeException("Comment not found");
        }
        if(!comment.get().getUser().getEmail().equals(user.getEmail())) {
            log.warn("[SERVICE] Comment deletion failed: User {} is not authorized to delete comment owned by {}", email, comment.get().getUser().getEmail());
            throw new RuntimeException("You are not allowed to delete this comment");
        }
        Post post = comment.get().getPost();
        post.setCommentCount(post.getCommentCount()-1);
        postRepo.save(post);
        commentRepo.delete(comment.get());


        log.info("[SERVICE] Comment ID {} deleted from DB", commentID);
    }

    public Page<CommentResponse> getCommentByPostId(Long postId, int page,int size) {
        log.info("[SERVICE] Fetching page {} of size {} comments for postId: {}", page, size, postId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt"));
        Page<Comment> comments =  commentRepo.findByPostId(postId, pageable);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email;
        if(authentication!=null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            email = authentication.getName();
        } else {
            email = null;
        }
        log.info("[SERVICE] Active user for fetching comments: {}", email);
        User user = userRepo.findByEmail(email);
        return comments.map(comment->{
            long upvotes = comment.getUpvotes();
            long downvotes = comment.getDownvotes();

            long voteCount = upvotes-downvotes;


            VoteType voteType=null;

            if(email!=null) {
                Optional<CommentVote> commentVote = commentVoteRepo.findByUserAndComment(user,comment);
                voteType = commentVote.map(CommentVote::getVoteType).orElse(null);
            }


            Long parentCommentId = null;

            if(comment.getParentComment() != null){
                parentCommentId = comment.getParentComment().getId();
            }


            return new CommentResponse(comment.getUser().getUsername(),comment.getId(),comment.getContent(),comment.getCreatedAt(),parentCommentId,voteCount,voteType);
        });

    }

    public CommentResponse maptoCommentResponse(Comment comment) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        long upvotes = comment.getUpvotes();
        long downvotes = comment.getDownvotes();

        long voteCount = upvotes-downvotes;

        Optional<CommentVote> commentVote = commentVoteRepo.findByUserAndComment(user,comment);

        Long parentCommentId = null;

        if(comment.getParentComment() != null){
            parentCommentId = comment.getParentComment().getId();
        }
        VoteType voteType = commentVote.map(CommentVote::getVoteType).orElse(null);
        return new CommentResponse(comment.getUser().getUsername(),comment.getId(),comment.getContent(),comment.getCreatedAt(),parentCommentId,voteCount,voteType);
    }

}
