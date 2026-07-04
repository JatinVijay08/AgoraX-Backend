package com.jatin.forum.repository;

import com.jatin.forum.dto.NotificationResponse;
import com.jatin.forum.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification, Long> {
    // Query for fetching NotificationResponse DTO

    @Query("SELECT new com.jatin.forum.dto.NotificationResponse(u.username,n.type,n.createdAt,n.read,n.postId,n.commentId ) FROM Notification n join User u on u.id=n.creatorId WHERE n.receiverId= :receiverId and n.createdAt< :cursor ORDER BY n.createdAt DESC ")
    public List<NotificationResponse> findAllNotifications(@Param("receiverId") Long receiverId, @Param("cursor") Instant cursorCreatedAt, Pageable pageable);

    // @Modifying for Specifying DB changes
    @Modifying
    @Query("UPDATE Notification n SET n.read=true WHERE n.receiverId= :receiverId and n.read=false")
    public void markAllNotificationsAsRead(@Param("receiverId") Long receiverId);


   public Integer countNotificationByReceiverIdAndReadIsFalse(Long receiverId, boolean read);
}
