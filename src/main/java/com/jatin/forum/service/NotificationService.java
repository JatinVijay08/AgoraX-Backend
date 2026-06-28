package com.jatin.forum.service;

import com.jatin.forum.dto.NotificationFeedResponse;
import com.jatin.forum.dto.NotificationResponse;
import com.jatin.forum.entity.*;
import com.jatin.forum.repository.NotificationRepo;
import com.jatin.forum.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;



@Service
public class NotificationService {


    private final NotificationRepo notificationRepo;
    private final UserRepo userRepo;

    public NotificationService(NotificationRepo notificationRepo, UserRepo userRepo) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
    }

    // create a notification: method : as it is a command and not a query
    // -> void return type
    public void createNotification(Post post,User creator,NotificationType notificationType) {
    // Notification now received after upvote
        // check and verify
        // no self notifications
        if(!post.getUser().getId().equals(creator.getId())){
            // create notification
            Notification notification = new Notification();
            notification.setCreatorId(creator.getId());
            notification.setReceiverId(post.getUser().getId());
            notification.setPostId(post.getId());
            notification.setType(notificationType);
            notificationRepo.save(notification);
        }

    }

    private static final int Max_Page_Size = 20;

    public NotificationFeedResponse getNotifications(Instant cursor,int limit){
        // first fetch the notificationList
        //get Reciever Id
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);



        if(limit<=0){
            throw new IllegalArgumentException("limit can't be less than 0");
        }

        limit = Math.min(limit,Max_Page_Size);

        long receiverId = user.getId();
        Instant newCursor = cursor == null ? Instant.now() : cursor;

        List<NotificationResponse> responseList = notificationRepo.findAllNotifications(receiverId,newCursor, PageRequest.of(0, limit+1));
        // list returned
        if(responseList.isEmpty()){
            return new NotificationFeedResponse(List.of(),false,null);
        }

        boolean hasMore = responseList.size() > limit;
        if(hasMore){
            // drop the last element of the list
            responseList.remove(responseList.size()-1);
        }

        Instant lastTimestamp = responseList.getLast().createdAt();
        return new NotificationFeedResponse(responseList,hasMore,lastTimestamp);

    }

    @Transactional
    public void markAllNotificationAsRead(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if(user==null){
            throw new IllegalArgumentException("User not found");
        }
        Long receiverId = user.getId();
        notificationRepo.markAllNotificationsAsRead(receiverId);

    }




}
