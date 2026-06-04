package com.jatin.forum.security;

import com.jatin.forum.JwtUtil;
import com.jatin.forum.entity.User;
import com.jatin.forum.repository.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepo userRepo;


    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepo userRepo) {
        this.jwtUtil = jwtUtil;
        this.userRepo = userRepo;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.info("[SECURITY] Checking authentication for URI: {}", request.getRequestURI());
        String authHeader = request.getHeader("Authorization");

        // ✅ IMPORTANT: skip if no Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("[SECURITY] No Bearer token found in Authorization header. Proceeding unauthenticated.");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        log.info("[SECURITY] Bearer token found. Validating token...");

        // if jwt token is not valid: go unauthenticated
        if(!jwtUtil.isValid(token)){
            log.warn("[SECURITY] Invalid JWT token. Proceeding unauthenticated.");
            filterChain.doFilter(request, response);
            return;
        }


        String email = jwtUtil.extractEmail(token);
        log.info("[SECURITY] Valid JWT token found for email: {}. Checking user in database...", email);

        User user = userRepo.findByEmail(email);
        if (user == null) {
            log.error("[SECURITY] User with email: {} not found. Authentication failed.", email);
            throw new RuntimeException("Invalid email or password");
        }


        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        List.of()
                );

        log.info("[SECURITY] User {} successfully authenticated. Setting SecurityContext.", email);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }


}
