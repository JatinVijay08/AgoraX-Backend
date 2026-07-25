package com.jatin.forum.dto;

import com.jatin.forum.entity.VoteType;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull(message = "Vote type is required")
        VoteType voteType
) {
}
