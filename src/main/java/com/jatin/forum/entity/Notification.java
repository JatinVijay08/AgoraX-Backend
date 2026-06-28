package com.jatin.forum.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp// automatically creates timestamp
    private Instant createdAt;

    @Column(nullable = false)
    private Long creatorId;

    @Column(nullable = false)
    private Long receiverId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // optional
    private Long postId; // for navigating to the post
    private Long commentId; // for navigating to the comment


    // for tracking read or unread status
   private boolean read = false; // default false value



}

