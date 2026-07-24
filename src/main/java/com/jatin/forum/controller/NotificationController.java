package com.jatin.forum.controller;

import com.jatin.forum.dto.NotificationFeedResponse;
import com.jatin.forum.service.NotificationService;
import org.springframework.http.HttpStatus;
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
    public NotificationFeedResponse getNotifications(@RequestParam("limit") int limit,@RequestParam(value = "cursor",required = false) Instant cursor){
        return notificationService.getNotifications(cursor, limit);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllNotificationAsRead(){
        notificationService.markAllNotificationAsRead();
    }

    @GetMapping("/unread-count")
    public Integer getUnreadNotificationsCount(){
       return notificationService.countUnreadNotifications();
    }




}
