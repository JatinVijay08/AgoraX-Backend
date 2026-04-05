package com.jatin.forum.repository;

import java.time.Instant;
import java.util.List;
import com.jatin.forum.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepo extends JpaRepository<Post, Long> {


    @Query("select p from Post p where p.createdAt < :cursor ORDER BY p.createdAt DESC ")
    List<Post> findPostNew(@Param("cursor") Instant cursor, Pageable pageable);

    List<Post> getPostByUserId(Long id);

    @Query("select p from Post p where p.createdAt > :time")
    List<Post> findPostRecent(@Param("time")Instant time);

    Post findPostByMediaPublicId(String publicId);
}
