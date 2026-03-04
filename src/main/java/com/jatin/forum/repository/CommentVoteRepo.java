package com.jatin.forum.repository;

import com.jatin.forum.entity.Comment;
import com.jatin.forum.entity.CommentVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentVoteRepo extends JpaRepository<CommentVote,Long> {
    Optional<CommentVote> findById(long commentId, Long id);

    long countByCommentAndVoteType(Comment comment, VoteType voteType);

    Optional<CommentVote> findByUserAndComment(User user, Comment comment);
}
