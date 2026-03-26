package com.jatin.exhale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUsernameRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        String username
) {}
