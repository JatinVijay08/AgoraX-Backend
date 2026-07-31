package com.jatin.forum.service;

import com.jatin.forum.utilities.JwtUtil;
import com.jatin.forum.dto.LoginRequest;
import com.jatin.forum.dto.LoginResponseDto;
import com.jatin.forum.dto.RegisterRequest;
import com.jatin.forum.entity.AuthProvider;
import com.jatin.forum.entity.User;
import com.jatin.forum.exception.InvalidCredentialsException;
import com.jatin.forum.exception.ResourceNotFoundException;
import com.jatin.forum.exception.UserAlreadyExistsException;
import com.jatin.forum.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private GoogleTokenVerifierService googleTokenVerifierService;
    @Mock private UserRepo userRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .password("encoded_password")
                .username("testuser")
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    @Test
    @DisplayName("1. Register New User -> Encodes password and saves user")
    void register_NewUser_ShouldEncodePasswordAndSave() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        authService.register(request);

        verify(userRepo, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("2. Register Existing User -> Throws UserAlreadyExistsException")
    void register_ExistingUser_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepo, never()).save(any());
    }

    @Test
    @DisplayName("3. Login Valid Credentials -> Generates and returns JWT token")
    void login_ValidCredentials_ShouldReturnJwtToken() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("mock_jwt_token");

        LoginResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.token());
        assertEquals("testuser", response.username());
    }

    @Test
    @DisplayName("4. Login Invalid Password -> Throws InvalidCredentialsException")
    void login_InvalidPassword_ShouldThrowException() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("5. Google Login New User -> Verifies ID token and registers Google user")
    void googleLogin_NewUser_ShouldVerifyAndRegister() {
        String idToken = "google_id_token";
        Map<String, Object> payload = Map.of(
                "email", "google@example.com",
                "name", "Google User",
                "sub", "google_12345"
        );

        when(googleTokenVerifierService.verify(idToken)).thenReturn(payload);
        when(userRepo.findByEmail("google@example.com")).thenReturn(Optional.empty());
        when(userRepo.existsByUsername(anyString())).thenReturn(false);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("google_jwt_token");

        LoginResponseDto response = authService.googleLogin(idToken);

        assertNotNull(response);
        assertEquals("google_jwt_token", response.token());
        verify(userRepo, times(1)).save(any(User.class));
    }
}
