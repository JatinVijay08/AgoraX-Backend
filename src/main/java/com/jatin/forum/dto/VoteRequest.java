package com.jatin.forum.dto;

import com.jatin.forum.entity.VoteType;

public record VoteRequest(
        VoteType voteType
) {
}
