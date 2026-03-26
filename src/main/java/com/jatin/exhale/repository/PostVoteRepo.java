package com.jatin.exhale.repository;

import com.jatin.exhale.entity.Post;
import com.jatin.exhale.entity.PostVote;
import com.jatin.exhale.entity.User;
import com.jatin.exhale.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface PostVoteRepo extends JpaRepository<PostVote, Long> {

    Optional<PostVote> findByUserAndPost(User user, Post post);

    long countByPostAndVoteType(Post post, VoteType voteType);

    @Modifying
    void deleteByPostId(Long postId);

    long countByPostAndCreatedAtAfter(Post post, java.time.Instant createdAt);
}
