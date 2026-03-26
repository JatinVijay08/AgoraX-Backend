package com.jatin.exhale.dto;

import java.util.List;

public record PostFeedResponse(
        List<PostResponse> posts,
        String nextCursor, // timestamp of the last post seen on feed
        Boolean hasMore
){
}
