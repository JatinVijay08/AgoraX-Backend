package com.jatin.forum.service;

import com.jatin.forum.dto.NotificationCreatedEvent;
import com.jatin.forum.dto.NotificationFeedResponse;
import com.jatin.forum.dto.NotificationResponse;
import com.jatin.forum.entity.*;
import com.jatin.forum.repository.NotificationRepo;
import com.jatin.forum.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock private NotificationRepo notificationRepo;
    @Mock private UserRepo userRepo;
    @Mock private ApplicationEventPublisher applicationEventPublisher;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private NotificationService notificationService;

    private User creator;
    private User receiver;
    private Post post;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");
        creator.setEmail("creator@email.com");

        receiver = new User();
        receiver.setId(2L);
        receiver.setUsername("receiver");
        receiver.setEmail("receiver@email.com");

        post = new Post("Title", "Content", receiver);
        post.setId(10L);
    }

    @Test
    @DisplayName("1. Create Notification Different User -> Saves notification and publishes WebSocket Event")
    void createNotification_DifferentUser_ShouldSaveAndPublishEvent() {
        Notification notification = new Notification();
        notification.setCreatorId(creator.getId());
        notification.setReceiverId(receiver.getId());
        notification.setPostId(post.getId());
        notification.setType(NotificationType.POST_LIKE);

        when(notificationRepo.save(any(Notification.class))).thenReturn(notification);

        notificationService.createNotification(post, creator, NotificationType.POST_LIKE);

        verify(notificationRepo, times(1)).save(any(Notification.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    @DisplayName("2. Self Notification -> Should NOT save or publish notification when user likes own post")
    void createNotification_SelfPost_ShouldIgnoreNotification() {
        // Creator is also post author
        notificationService.createNotification(post, receiver, NotificationType.POST_LIKE);

        verifyNoInteractions(notificationRepo);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("3. Create Reply Notification -> Notifies parent comment author correctly")
    void createReplyNotification_ShouldNotifyParentCommentAuthor() {
        Comment parentComment = new Comment("Parent", receiver, post);
        parentComment.setId(50L);

        Comment replyComment = new Comment("Reply", creator, post);
        replyComment.setId(51L);
        replyComment.setParentComment(parentComment);

        Notification saved = new Notification();
        saved.setCreatorId(creator.getId());
        saved.setReceiverId(receiver.getId());

        when(notificationRepo.save(any(Notification.class))).thenReturn(saved);

        notificationService.createReplyNotification(post, creator, replyComment, parentComment);

        verify(notificationRepo, times(1)).save(any(Notification.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    @DisplayName("4. Get Notifications -> Returns cursor paginated notification feed")
    void getNotifications_ShouldReturnPaginatedFeed() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(receiver));

        Instant now = Instant.now();
        NotificationResponse n1 = new NotificationResponse("creator", NotificationType.POST_LIKE, now, false, 10L, null);
        List<NotificationResponse> list = new ArrayList<>(List.of(n1));

        when(notificationRepo.findAllNotifications(eq(2L), any(Instant.class), any(Pageable.class))).thenReturn(list);

        NotificationFeedResponse response = notificationService.getNotifications(null, 10);

        assertNotNull(response);
        assertEquals(1, response.list().size());
        assertFalse(response.hasMore());
    }

    @Test
    @DisplayName("5. Mark All Notifications Read -> Calls repository bulk update for receiver")
    void markAllNotificationAsRead_ShouldCallRepository() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(receiver));

        notificationService.markAllNotificationAsRead();

        verify(notificationRepo, times(1)).markAllNotificationsAsRead(2L);
    }

    @Test
    @DisplayName("6. Count Unread Notifications -> Returns integer count of unread notifications")
    void countUnreadNotifications_ShouldReturnCount() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(receiver));
        when(notificationRepo.countNotificationByReceiverIdAndReadIsFalse(2L, false)).thenReturn(5);

        Integer count = notificationService.countUnreadNotifications();

        assertEquals(5, count);
    }
}
