package com.jatin.forum.dto;

public record CreateCommentRequest(
        String content,
        Long parentId
) {
}
