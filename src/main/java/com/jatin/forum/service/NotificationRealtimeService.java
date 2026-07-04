package com.jatin.forum.service;

import com.jatin.forum.dto.NotificationResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationRealtimeService {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public NotificationRealtimeService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public void sendNotification(String receiverIdentifier, NotificationResponse notificationResponse) {
        simpMessagingTemplate.convertAndSendToUser(
                receiverIdentifier,
                "/queue/notifications",
                notificationResponse
        );
    }
}
