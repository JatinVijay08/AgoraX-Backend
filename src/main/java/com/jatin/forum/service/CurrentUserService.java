package com.jatin.forum.service;

import com.jatin.forum.entity.User;
import com.jatin.forum.repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepo userRepo;

    public CurrentUserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        String email = authentication.getName();
        return userRepo.findByEmail(email);
    }
}
