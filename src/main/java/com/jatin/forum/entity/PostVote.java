package com.jatin.forum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "post_vote",uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id","user_id"})
})
public class PostVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    User user;

    @ManyToOne
    @JoinColumn(name="post_id",nullable = false)
    @Getter
    @Setter
    Post post;

    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    private VoteType voteType;

    public PostVote() {
    }
    public PostVote(User user, Post post, VoteType voteType) {
        this.user = user;
        this.post = post;
        this.voteType = voteType;
    }

    public VoteType getVoteType() {
        return voteType;
    }
    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
