package com.jatin.exhale.dto;

public record CreateCommentRequest(
        String content,
        Long parentId
) {
}
