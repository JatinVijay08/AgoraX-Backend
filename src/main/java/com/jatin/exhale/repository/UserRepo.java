package com.jatin.exhale.repository;

import com.jatin.exhale.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> {

    User findByEmail(@NotBlank String email);

    boolean existsByUsername(String username);
}
