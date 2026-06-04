package com.jatin.forum;


import com.jatin.forum.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {
    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateToken(User user) {
        log.info("[JWT] Generating token for user: {}", user.getEmail());
        String token = Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 1000 * 60 * 60)
                )
                .signWith(key)
                .compact();
        log.info("[JWT] Token generated successfully for {}", user.getEmail());
        return token;
    }

    public String extractEmail(String token) {
        log.info("[JWT] Extracting email from token");
        String email = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        log.info("[JWT] Extracted email from token: {}", email);
        return email;
    }

    public boolean isValid(String token) {
        log.info("[JWT] Validating token...");
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            log.info("[JWT] Token is valid");
            return true;
        } catch (Exception e) {
            log.warn("[JWT] Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
