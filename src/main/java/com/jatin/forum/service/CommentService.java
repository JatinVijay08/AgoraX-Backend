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
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CommentService {

    private final PostRepo postRepo;
    private final CommentRepo commentRepo;
    private final CommentVoteRepo commentVoteRepo;
    private final UserRepo userRepo;
    private final FeedCacheService feedCacheService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    public CommentService(PostRepo postRepo, CommentRepo commentRepo, CommentVoteRepo commentVoteRepo, UserRepo userRepo, FeedCacheService feedCacheService, CurrentUserService currentUserService, NotificationService notificationService) {
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.commentVoteRepo = commentVoteRepo;
        this.userRepo = userRepo;
        this.feedCacheService = feedCacheService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    public CommentResponse CreateComment(Long postID, CreateCommentRequest createCommentRequest) {
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Post> post = postRepo.findById(postID);
        if (post.isEmpty()) {
            throw new RuntimeException("Post not found");
        }
        Comment comment = new Comment(createCommentRequest.content(), user, post.get());

        if(createCommentRequest.parentId() != null) {
            Comment parent = commentRepo.findById(createCommentRequest.parentId()).orElseThrow(()-> {
                return new RuntimeException("Parent comment not found");
            });
            comment.setParentComment(parent);
            commentRepo.save(comment);
            notificationService.createReplyNotification(post.get(), user, comment, parent);
        } else {
            commentRepo.save(comment);
            notificationService.createCommentNotification(post.get(), user, comment);
        }

        long newCommentCount = post.get().getCommentCount();
        post.get().setCommentCount(newCommentCount+1);
        postRepo.save(post.get());

        // increment trending activity
        long trendingCount = feedCacheService.incrementActivity(FeedCacheService.TYPE_TRENDING);
        if(feedCacheService.shouldEvict(FeedCacheService.TYPE_TRENDING, trendingCount)){
            feedCacheService.resetActivity(FeedCacheService.TYPE_TRENDING);
            feedCacheService.evictFeed(FeedCacheService.TYPE_TRENDING, 10);
        }

        return maptoCommentResponse(comment);
    }

    public void deleteComment(Long commentID) {
        Optional<Comment> comment = commentRepo.findById(commentID);
        User user = currentUserService.getCurrentUser().orElseThrow(() -> new RuntimeException("User not found"));
        if (comment.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }
        if(!comment.get().getUser().getEmail().equals(user.getEmail())) {
            throw new RuntimeException("You are not allowed to delete this comment");
        }
        Post post = comment.get().getPost();
        post.setCommentCount(post.getCommentCount()-1);
        postRepo.save(post);
        commentRepo.delete(comment.get());

        feedCacheService.evictFeed(FeedCacheService.TYPE_TRENDING, 10);
        feedCacheService.resetActivity(FeedCacheService.TYPE_TRENDING);


    }

    public Page<CommentResponse> getCommentByPostId(Long postId, int page,int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt"));
        Page<Comment> comments =  commentRepo.findByPostId(postId, pageable);
        
        User user = currentUserService.getCurrentUser().orElse(null);
        return comments.map(comment->{
            long upvotes = comment.getUpvotes();
            long downvotes = comment.getDownvotes();

            long voteCount = upvotes-downvotes;


            VoteType voteType=null;

            if(user != null) {
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
        return new CommentResponse(comment.getUser().getUsername(),comment.getId(),comment.getContent(),comment.getCreatedAt(),parentCommentId,voteCount,voteType);
    }



}
