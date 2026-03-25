package com.jatin.forum.repository;

import com.jatin.forum.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> {

    User findByEmail(@NotBlank String email);

    boolean existsByUsername(String username);
}
