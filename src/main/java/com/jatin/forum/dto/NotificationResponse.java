package com.jatin.forum.dto;

import com.jatin.forum.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        String creatorName,
        NotificationType notificationType,
        Instant createdAt,
        boolean read,
        Long postId,
        Long commentId
) {
}
