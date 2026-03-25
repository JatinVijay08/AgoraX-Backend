package com.jatin.forum.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;


@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Table(name="posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(nullable = false,columnDefinition = "TEXT")
    private String content;

    private Instant createdAt;

    public Post(String title, String content, User user) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }


    public String getTitle() {
        return title;
    }


    public String getContent() {
        return content;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }



    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;



    public User getUser() {
        return user;
    }



}
