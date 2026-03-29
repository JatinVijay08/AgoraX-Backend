package com.jatin.forum.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePostRequest(
       @NotBlank(message="Title cannot be empty")
        String title,
        @NotBlank(message = "Content cannot be empty")
        String content
) {}
