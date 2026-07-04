package com.jatin.forum.dto;

import com.jatin.forum.entity.Notification;
import com.jatin.forum.entity.User;

public record NotificationCreatedEvent(
        String receiverEmail,
        NotificationResponse notificationResponse
) {
}
