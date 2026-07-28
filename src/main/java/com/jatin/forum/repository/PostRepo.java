package com.jatin.forum.repository;

import java.time.Instant;
import java.util.List;
import com.jatin.forum.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepo extends JpaRepository<Post, Long> {


    @Query("select p from Post p where p.createdAt < :cursor ORDER BY p.createdAt DESC ")
    List<Post> findPostNew(@Param("cursor") Instant cursor, Pageable pageable);

    List<Post> getPostByUserId(Long id);

    @Query("select p from Post p where p.createdAt > :time")
    List<Post> findPostRecent(@Param("time")Instant time);

    Post findPostByMediaPublicId(String publicId);

    @Modifying
    @Query("UPDATE Post p SET p.upvotesCount = p.upvotesCount+1 where p.id = :postId")
    void incrementUpvoteCount(@Param("postId")Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.upvotesCount = p.upvotesCount-1 where p.id = :postId")
    void decrementUpvoteCount(@Param("postId")Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.downvotesCount = p.downvotesCount+1 where p.id = :postId")
    void incrementDownvoteCount(@Param("postId")Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.downvotesCount = p.downvotesCount-1 where p.id = :postId")
    void decrementDownvoteCount(@Param("postId")Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount+1 where p.id = :postId")
    void incrementCommentCount(@Param("postId")Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount-1 where p.id = :postId")
    void decrementCommentCount(@Param("postId")Long postId);



}
