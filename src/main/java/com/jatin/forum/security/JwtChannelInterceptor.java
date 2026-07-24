package com.jatin.forum.security;

import com.jatin.forum.JwtUtil;
import com.jatin.forum.entity.User;
import com.jatin.forum.repository.UserRepo;
import com.jatin.forum.service.UserService;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

public class JwtChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;
    private final UserRepo  userRepo;

    public JwtChannelInterceptor(JwtUtil jwtUtil, UserRepo userRepo) {
        this.jwtUtil = jwtUtil;

        this.userRepo = userRepo;
    }

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if(accessor==null){
            return message;
        }
        if(accessor.getCommand()==StompCommand.CONNECT){
            // extract the header
            String header = accessor.getFirstNativeHeader("Authorization");
            if(header==null){
                return message;
            }
            if(header.startsWith("Bearer ")){
                String token = header.substring(7);
                if(jwtUtil.isValid(token)){
                    String email = jwtUtil.extractEmail(token);
                    User user = userRepo.findByEmail(email).orElse(null);
                    if(user==null){
                        return message;
                    }
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken
                            (user.getEmail(),
                                   null, List.of()
                            );
                    accessor.setUser(authentication);
                   // authentication object is now made
                }

            }
        }
        return message;
    }
}
