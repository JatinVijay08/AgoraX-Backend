package com.jatin.forum.repository;

import com.jatin.forum.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface UserRepo extends JpaRepository<User, Long> {

    java.util.Optional<User> findByEmail(@NotBlank String email);

    boolean existsByUsername(String username);

    java.util.Optional<User> findByUsername(String username);

    List<User> findUserByLastLoginAtBefore(Instant lastLoginAtBefore, Sort sort, Limit limit);

    List<User> findUserByLastLoginAtBefore(Instant lastLoginAtBefore);
}
