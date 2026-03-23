package com.jatin.forum.dto;

import com.jatin.forum.entity.VoteType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record PostResponse(
        String username,
        Long id,
        String title,
        String content,
        long voteCount,
        long commentCount,
        VoteType userVote,
        Instant createdAt
){}
