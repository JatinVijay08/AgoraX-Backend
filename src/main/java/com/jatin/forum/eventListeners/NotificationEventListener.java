package com.jatin.forum.eventListeners;

import com.jatin.forum.dto.NotificationCreatedEvent;
import com.jatin.forum.service.NotificationRealtimeService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {
    private final NotificationRealtimeService notificationRealtimeService;

    public NotificationEventListener(NotificationRealtimeService notificationRealtimeService) {
        this.notificationRealtimeService = notificationRealtimeService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationCreatedEvent event) {
        System.out.println("Notification Event Listener Fired up!!");
        notificationRealtimeService.sendNotification(event.receiverEmail(), event.notificationResponse());
    }


}
