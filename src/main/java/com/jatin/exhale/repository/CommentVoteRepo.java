package com.jatin.exhale.repository;

import com.jatin.exhale.entity.Comment;
import com.jatin.exhale.entity.CommentVote;
import com.jatin.exhale.entity.User;
import com.jatin.exhale.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentVoteRepo extends JpaRepository<CommentVote,Long> {
    Optional<CommentVote> findById(long commentId, Long id);

    long countByCommentAndVoteType(Comment comment, VoteType voteType);

    Optional<CommentVote> findByUserAndComment(User user, Comment comment);
}
