package com.jatin.forum.dto;

import java.time.Instant;
import java.util.List;

public record CachedFeed(
        List<CachedPost> cachedPostList,
        boolean hasMore,
        Instant cursor
){}
