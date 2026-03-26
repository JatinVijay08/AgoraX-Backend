package com.jatin.exhale.dto;

import com.jatin.exhale.entity.VoteType;

public record VoteRequest(
        VoteType voteType
) {
}
