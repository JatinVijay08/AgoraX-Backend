package com.jatin.forum.dto;

import java.time.Instant;
import java.util.List;

public record NotificationFeedResponse(
        List<NotificationResponse> list,
        boolean hasMore,
        Instant cursor
) {
}
