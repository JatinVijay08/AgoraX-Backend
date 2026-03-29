package com.jatin.forum.dto;

import com.jatin.forum.entity.VoteType;

import java.time.Instant;


public record PostResponse(
        String username,
        Long id,
        String title,
        String content,
        long voteCount,
        long commentCount,
        VoteType userVote,
        Instant createdAt,
        double hotScore,
        String mediaUrl,
        String mediaType) {}
