package com.jatin.forum.entity;

public enum VoteType {
    upvote(1), downvote(-1);

    private final int value;

    VoteType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
