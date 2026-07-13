package com.jatin.forum.dto;

import com.jatin.forum.entity.User;

import java.time.Instant;

public record CachedPost(
        Long id,
        String title,
        String content,
        Instant createdAt,
        String mediaUrl,
        String mediaType,
        String mediaPublicId,
        String creatorUsername,
        long commentCount,
        long upvotesCount,
        long downvotesCount
){}
