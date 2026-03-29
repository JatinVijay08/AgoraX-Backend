package com.jatin.forum.dto;

import java.time.Instant;

public record UserResponse(
        String username,
        String email,
        Instant createdAt
) {
}
