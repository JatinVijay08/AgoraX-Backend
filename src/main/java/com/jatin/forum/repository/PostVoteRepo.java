package com.jatin.forum.repository;

import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostVoteRepo extends JpaRepository<PostVote, Long> {

    Optional<PostVote> findByUserAndPost(User user, Post post);

    long countByPostAndVoteType(Post post, VoteType voteType);

    @Modifying
    void deleteByPostId(Long postId);

    long countByPostAndCreatedAtAfter(Post post, java.time.Instant createdAt);

    List<PostVote> findPostVotesByCreatedAtAfter(Instant createdAtAfter);
}
