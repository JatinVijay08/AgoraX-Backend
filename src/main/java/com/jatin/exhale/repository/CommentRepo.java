package com.jatin.exhale.repository;

import com.jatin.exhale.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;


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

    long countByPostAndCreatedAtAfter(com.jatin.exhale.entity.Post post, java.time.Instant createdAt);
}
