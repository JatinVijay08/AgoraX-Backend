package com.jatin.forum.dto;

import java.time.Instant;

public record UserProfileResponse(
        String username,
        String email,
        Instant createdAt,
        long postCount,
        long commentCount,
        long karma
) {
}
