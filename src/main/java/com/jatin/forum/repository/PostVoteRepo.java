package com.jatin.forum.repository;

import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostVoteRepo extends JpaRepository<PostVote, Long> {

    Optional<PostVote> findByUserAndPost(User user, Post post);

    long countByPostAndVoteType(Post post, VoteType voteType);

    @Modifying
    void deleteByPostId(Long postId);

    long countByPostAndCreatedAtAfter(Post post, java.time.Instant createdAt);

    List<PostVote> findPostVotesByCreatedAtAfter(Instant createdAtAfter);

    List<PostVote> findByUserAndPostIdIn(User user, Collection<Long> postIds);

    Collection<Long> post(Post post);

    @Query("SELECT pv.post.id, SUM(CASE WHEN pv.voteType = 'upvote' THEN 1 ELSE -1 END) FROM PostVote pv WHERE pv.post.id IN :postIds GROUP BY pv.post.id")
    List<Object[]> getAggregateVotesForPosts(@Param("postIds") Collection<Long> postIds);

}
