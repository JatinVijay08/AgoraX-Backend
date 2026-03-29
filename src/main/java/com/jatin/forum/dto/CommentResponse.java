package com.jatin.forum.dto;

import com.jatin.forum.entity.VoteType;

import java.time.Instant;

public record CommentResponse(
        String username,
        Long id,
        String content,
        Instant createdAt,
        Long parentComment,
        long voteCount,
        VoteType voteType
) {
}
