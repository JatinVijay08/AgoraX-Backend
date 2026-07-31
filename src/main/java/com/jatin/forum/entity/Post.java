package com.jatin.forum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Instant createdAt;

    // Media fields — stored as Cloudinary URLs
    @Setter
    private String mediaUrl;

    @Setter
    private String mediaType; // "image" or "video"

    @Setter
    private String mediaPublicId; // Cloudinary public_id for deletion

    public Post(String title, String content, User user) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.createdAt = Instant.now();
    }

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Setter
    private long commentCount;

    @Setter
    private long upvotesCount;

    @Setter
    private long downvotesCount;

}
