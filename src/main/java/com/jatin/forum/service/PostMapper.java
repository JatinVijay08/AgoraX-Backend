package com.jatin.forum.service;

import com.jatin.forum.dto.CachedPost;
import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostVoteRepo;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

@Component
public class PostMapper {

    private final PostVoteRepo postVoteRepo;
    private final CommentRepo commentRepo;

    public PostMapper(PostVoteRepo postVoteRepo, CommentRepo commentRepo) {
        this.postVoteRepo = postVoteRepo;
        this.commentRepo = commentRepo;
    }

    public PostResponse mapToPostResponse(Post post, HashMap<Long, VoteType> voteTypeHashMap) {
        long upvotes = post.getUpvotesCount();
        long downvotes = post.getDownvotesCount();
        long votes = upvotes - downvotes;
        long commentCount = post.getCommentCount();

        VoteType voteType = voteTypeHashMap.getOrDefault(post.getId(), null);
        User user = post.getUser();

        long hoursOld = Duration.between(post.getCreatedAt(), Instant.now()).toHours();
        double hotScore = getHotScoreFromValue(votes, hoursOld);

        return new PostResponse(user.getUsername(), post.getId(), post.getTitle(), post.getContent(), votes, commentCount, voteType, post.getCreatedAt(), hotScore, post.getMediaUrl(), post.getMediaType());
    }

    public CachedPost mapToCachedPostFromPost(Post post) {
        return new CachedPost(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getMediaUrl(),
                post.getMediaType(),
                post.getMediaPublicId(),
                post.getUser().getUsername(),
                post.getCommentCount(),
                post.getUpvotesCount(),
                post.getDownvotesCount()
        );
    }

    public PostResponse mapToPostResponseFromCachePost(CachedPost cachedPost, HashMap<Long, VoteType> voteTypeHashMap) {
        long upvotes = cachedPost.upvotesCount();
        long downvotes = cachedPost.downvotesCount();
        long votes = upvotes - downvotes;
        long commentCount = cachedPost.commentCount();

        VoteType voteType = voteTypeHashMap.getOrDefault(cachedPost.id(), null);
        String username = cachedPost.creatorUsername();

        long hoursOld = Duration.between(cachedPost.createdAt(), Instant.now()).toHours();
        double hotScore = getHotScoreFromValue(votes, hoursOld);

        return new PostResponse(username, cachedPost.id(), cachedPost.title(), cachedPost.content(), votes, commentCount, voteType, cachedPost.createdAt(), hotScore, cachedPost.mediaUrl(), cachedPost.mediaType());
    }

    public double getHotScoreFromValue(long votes, long hoursOld) {
        return votes / Math.pow(hoursOld + 2, 1.5);
    }

    public double getHotScorePost(Post post) {
        long upvotes = post.getUpvotesCount();
        long downvotes = post.getDownvotesCount();
        long votes = upvotes - downvotes;
        long hoursOld = Duration.between(post.getCreatedAt(), Instant.now()).toHours();
        return getHotScoreFromValue(votes, hoursOld);
    }

    public double getTrendingScore(Post post) {
        Instant sixHoursAgo = Instant.now().minus(6, ChronoUnit.HOURS);
        long recentVotes = postVoteRepo.countByPostAndCreatedAtAfter(post, sixHoursAgo);
        long recentComments = commentRepo.countByPostAndCreatedAtAfter(post, sixHoursAgo);
        return recentVotes + (recentComments * 2);
    }
}
