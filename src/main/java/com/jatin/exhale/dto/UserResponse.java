package com.jatin.exhale.dto;

import java.time.Instant;

public record UserResponse(
        String displayName,
        String email,
        Instant createdAt
) {
}
