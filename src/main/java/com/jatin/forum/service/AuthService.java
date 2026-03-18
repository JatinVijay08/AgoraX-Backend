package com.jatin.forum.service;

import com.jatin.forum.JwtUtil;
import com.jatin.forum.dto.LoginRequest;
import com.jatin.forum.dto.LoginResponseDto;
import com.jatin.forum.dto.RegisterRequest;
import com.jatin.forum.entity.User;
import com.jatin.forum.repository.UserRepo;
import com.jatin.forum.exception.InvalidCredentialsException;
import com.jatin.forum.exception.ResourceNotFoundException;
import com.jatin.forum.exception.UserAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepo userRepo;

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegisterRequest registerRequest) {
        if (userRepo.findByEmail(registerRequest.email()) != null) {
            throw new UserAlreadyExistsException("User is already Registered with this Email");
        }

        String hashedPassword = passwordEncoder.encode(registerRequest.password());
        User user = new User(registerRequest.username(), hashedPassword, registerRequest.email());
        userRepo.save(user);
    }

    public LoginResponseDto login(LoginRequest loginRequest) {
        User user = userRepo.findByEmail(loginRequest.email());
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        if(!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid Credentials");
        }

        String token = jwtUtil.generateToken(user);
        String username = user.getUsername();
        return new LoginResponseDto(token,username);
    }




}
