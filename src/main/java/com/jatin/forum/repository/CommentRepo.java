package com.jatin.forum.repository;

import com.jatin.forum.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;


public interface CommentRepo extends JpaRepository<Comment, Long> {
    @Query("""
          SELECT c from Comment c
          Join fetch c.user
          where c.post.id = :postId
""")
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    long countByPostId(Long id);

    @Modifying
    void deleteByPostId(Long postId);

    Long id(Long id);

    long countByPostAndCreatedAtAfter(com.jatin.forum.entity.Post post, java.time.Instant createdAt);

    List<Comment> findCommentsByCreatedAtAfter(Instant createdAtAfter);

    @Modifying
    @Query("UPDATE Comment c SET c.upvotes = c.upvotes+1 where c.id = :commentId")
    void incrementUpvoteCount(@Param("commentId")Long commentId);

    @Modifying
    @Query("UPDATE Comment c SET c.upvotes = c.upvotes-1 where c.id = :commentId")
    void decrementUpvoteCount(@Param("commentId")Long commentId);

    @Modifying
    @Query("UPDATE Comment c SET c.downvotes = c.downvotes+1 where c.id = :commentId")
    void incrementDownvoteCount(@Param("commentId")Long commentId);

    @Modifying
    @Query("UPDATE Comment c SET c.downvotes = c.downvotes-1 where c.id = :commentId")
    void decrementDownvoteCount(@Param("commentId")Long commentId);


}
