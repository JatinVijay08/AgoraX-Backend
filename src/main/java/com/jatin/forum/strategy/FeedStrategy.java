package com.jatin.forum.strategy;

import com.jatin.forum.dto.PostFeedResponse;
import com.jatin.forum.entity.User;

public interface FeedStrategy {
    PostFeedResponse fetchFeed(User user, int page,int limit,String cursor);
}

