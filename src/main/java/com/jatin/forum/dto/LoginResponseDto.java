package com.jatin.forum.dto;

public record LoginResponseDto(
        String token,
        String username
) {
}
