package com.jatin.forum.controller;


import com.jatin.forum.dto.GoogleAuthRequest;
import com.jatin.forum.dto.LoginRequest;
import com.jatin.forum.dto.LoginResponseDto;
import com.jatin.forum.dto.RegisterRequest;
import com.jatin.forum.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@Slf4j
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequest loginRequest) {

        LoginResponseDto loginResponseDto = authService.login(loginRequest);

        return ResponseEntity.ok(loginResponseDto);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponseDto> googleLogin(@Valid @RequestBody GoogleAuthRequest googleAuthRequest) {

        try {
            LoginResponseDto response = authService.googleLogin(googleAuthRequest.idToken());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new LoginResponseDto(null, e.getMessage()));
        }
    }
}
