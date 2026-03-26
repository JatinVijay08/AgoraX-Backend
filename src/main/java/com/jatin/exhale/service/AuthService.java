package com.jatin.exhale.service;

import com.jatin.exhale.JwtUtil;
import com.jatin.exhale.dto.LoginRequest;
import com.jatin.exhale.dto.LoginResponseDto;
import com.jatin.exhale.dto.RegisterRequest;
import com.jatin.exhale.entity.AuthProvider;
import com.jatin.exhale.entity.User;
import com.jatin.exhale.repository.UserRepo;
import com.jatin.exhale.exception.InvalidCredentialsException;
import com.jatin.exhale.exception.ResourceNotFoundException;
import com.jatin.exhale.exception.UserAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class AuthService {


    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final UserRepo userRepo;

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(GoogleTokenVerifierService googleTokenVerifierService, UserRepo userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegisterRequest registerRequest) {
        if (userRepo.findByEmail(registerRequest.email()) != null) {
            throw new UserAlreadyExistsException("User is already Registered with this Email");
        }

        String hashedPassword = passwordEncoder.encode(registerRequest.password());
        User user = User.builder()
                .email(registerRequest.email())
                .password(hashedPassword)
                .authProvider(AuthProvider.LOCAL)
                .created(Instant.now())
                .username(registerRequest.username())
                .build();
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
        String username = user.getDisplayName();
        return new LoginResponseDto(token,username);
    }

    public LoginResponseDto googleLogin(String idToken){

        Map<String,Object> payload = googleTokenVerifierService.verify(idToken);

        String email  =  (String) payload.get("email");
        String name  = (String) payload.get("name");
        String googleId = (String) payload.get("sub"); // unique user id

        User existingUser = userRepo.findByEmail(email);
        if(existingUser!=null){
            // user exists in the database
            if(existingUser.getAuthProvider()== AuthProvider.LOCAL){
                throw new RuntimeException("This email is already registered with email and password.Please Login normally");
            }

            // if AuthProvider is google,then issue jwt
            String jwt = jwtUtil.generateToken(existingUser);

            String username = existingUser.getDisplayName();
            return new LoginResponseDto(jwt,username);
        }

        // if existingUser is null, then register a new user
        User newUser = User.builder()
                .email(email)
                .password(null)
                .authProvider(AuthProvider.GOOGLE)
                .username(generateUsername(name))
                .googleId(googleId)
                .created(Instant.now())
        .build();

        userRepo.save(newUser);

        String jwt = jwtUtil.generateToken(newUser);
        return new LoginResponseDto(jwt,newUser.getDisplayName());
    }

    private String generateUsername(String name) {
        String base = name.toLowerCase().replaceAll("\\s+", "_");
        String candidate = base;
        int i = 1;
        while (userRepo.existsByUsername(candidate)){
            candidate = base + "_" + i++;
        }
        return candidate;
    }



}
