package com.jatin.forum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Entity
@Table(name="users")
@Builder
@Getter
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;


    @Column(nullable=false,unique=true)
    private String username;
    @Column(nullable=true)
    private String password;

    @Column(nullable=false,unique=true)
    private String email;


    private Instant created;


    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Getter
    @Column(name="google_id")
    private String googleId;

    protected User() {}


    public String getPassword() {
        return password;
    }
}