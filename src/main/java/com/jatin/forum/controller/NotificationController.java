package com.jatin.forum.controller;

import com.jatin.forum.dto.NotificationFeedResponse;
import com.jatin.forum.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;


@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationFeedResponse> getNotifications(@RequestParam("limit") int limit,@RequestParam(value = "cursor",required = false) Instant cursor){
        return ResponseEntity.ok(notificationService.getNotifications(cursor, limit));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllNotificationAsRead(){
        notificationService.markAllNotificationAsRead();
       return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadNotificationsCount(){
       return ResponseEntity.ok(notificationService.countUnreadNotifications());
    }




}
