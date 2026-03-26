package com.jatin.exhale.service;

import com.jatin.exhale.dto.PostFeedResponse;
import com.jatin.exhale.dto.PostResponse;
import com.jatin.exhale.entity.Post;

import com.jatin.exhale.entity.PostVote;
import com.jatin.exhale.entity.User;
import com.jatin.exhale.entity.VoteType;
import com.jatin.exhale.repository.CommentRepo;
import com.jatin.exhale.repository.PostRepo;
import com.jatin.exhale.repository.PostVoteRepo;
import com.jatin.exhale.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;


import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;



@Service
public class PostService {
    private final UserRepo userRepo;
    private final PostVoteRepo postVoteRepo;
    private final CommentRepo commentRepo;
    private PostRepo postRepo;

    public PostService(PostRepo postRepo, UserRepo userRepo, PostVoteRepo postVoteRepo, CommentRepo commentRepo) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
        this.postVoteRepo = postVoteRepo;
        this.commentRepo = commentRepo;
    }



    public PostFeedResponse getAllPosts(String sort, int page,int  limit, String cursor){
             Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
             boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser");

             if(sort.equals("new")) {
                 Instant cursorTime = (cursor != null) ? Instant.parse(cursor) : Instant.now();
                 List<Post> newList = postRepo.findPostNew(cursorTime, PageRequest.of(0, limit));
                 List<PostResponse> postResponses = newList.stream()
                         .map(post -> isAuthenticated ? mapToPostResponse(post) : mapToPostResponseUnauthenticated(post))
                         .toList();
                 Instant newCursor = postResponses.isEmpty() ? null : postResponses.get(postResponses.size() - 1).createdAt();
                 boolean hasMore = newList.size() == limit;
                 return new PostFeedResponse(postResponses, newCursor != null ? newCursor.toString() : null, hasMore);
             }
             if(sort.equals("hot")) {
                 Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
                 List<Post> hotList = postRepo.findPostRecent(sevenDaysAgo);
                 List<PostResponse> postResponses = hotList.stream()
                         .map(post -> isAuthenticated ? mapToPostResponse(post) : mapToPostResponseUnauthenticated(post))
                         .sorted(Comparator.comparingDouble(PostResponse::hotScore).reversed())
                         .skip((long) page * limit)
                         .limit(limit)
                         .toList();
                 boolean hasMore = postResponses.size() == limit;
                 return new PostFeedResponse(postResponses, null, hasMore);
             }
             if(sort.equals("trending")) {
                 Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
                 List<Post> recentTotal = postRepo.findPostRecent(twentyFourHoursAgo);
                 
                 List<PostResponse> postResponses = recentTotal.stream()
                         .sorted((p1, p2) -> Double.compare(getTrendingScore(p2), getTrendingScore(p1)))
                         .skip((long) page * limit)
                         .limit(limit)
                         .map(post -> isAuthenticated ? mapToPostResponse(post) : mapToPostResponseUnauthenticated(post))
                         .toList();
                         
                 boolean hasMore = postResponses.size() == limit;
                 return new PostFeedResponse(postResponses, null, hasMore);
             }
             return new PostFeedResponse(List.of(), null, false);
    }

    private double getTrendingScore(Post post) {
        Instant sixHoursAgo = Instant.now().minus(6, ChronoUnit.HOURS);
        long recentVotes = postVoteRepo.countByPostAndCreatedAtAfter(post, sixHoursAgo);
        long recentComments = commentRepo.countByPostAndCreatedAtAfter(post, sixHoursAgo);
        return recentVotes + (recentComments * 2);
    }

    public PostResponse createPost(String title, String content, String mediaUrl, String mediaType, String mediaPublicId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if(user==null){
            throw new RuntimeException("User not found");
        }
        Post post = new Post(title, content, user);
        post.setMediaUrl(mediaUrl);
        post.setMediaType(mediaType);
        post.setMediaPublicId(mediaPublicId);

        Post savedPost = postRepo.save(post);
        return mapToPostResponse(savedPost);
    }

    public PostResponse getPostById(Long id){
        Post post = postRepo.findById(id).orElseThrow(()->new RuntimeException("post not found")) ;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
             return mapToPostResponse(post);
        } else {
             return mapToPostResponseUnauthenticated(post);
        }
    }

    @Transactional
    public void deletePostById(Long postId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if(user==null){
            throw new RuntimeException("User not found");
        }
        Post post = postRepo.findById(postId).orElseThrow(()->new RuntimeException("post not found"));
        if(!post.getUser().getId().equals(user.getId())){
           throw new RuntimeException("Not allowed to delete post");
        }
        postVoteRepo.deleteByPostId(postId);
        commentRepo.deleteByPostId(postId);
        postRepo.deleteById(postId);

    }

    public PostResponse mapToPostResponse(Post post){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if(user==null){
            throw new RuntimeException("User not found");
        }
        long upvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.upvote);
        long downvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.downvote);

        long votes = upvotes-downvotes;
        long commentCount = commentRepo.countByPostId(post.getId());
        VoteType voteType = postVoteRepo.findByUserAndPost(user,post).map(PostVote::getVoteType).orElse(null);
        User user1 = post.getUser();
        long hoursOld = Duration.between(post.getCreatedAt(),Instant.now()).toHours();
        double hotScore = votes/Math.pow(hoursOld+2,1.5);

        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType,post.getCreatedAt(),hotScore, post.getMediaUrl(), post.getMediaType());

    }

    public PostResponse mapToPostResponseUnauthenticated(Post post){
        long upvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.upvote);
        long downvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.downvote);

        long votes = upvotes-downvotes;
        long commentCount = commentRepo.countByPostId(post.getId());
        VoteType voteType = null;
        User user1 = post.getUser();
        long hoursOld = Duration.between(post.getCreatedAt(),Instant.now()).toHours();
        double hotScore = votes/Math.pow(hoursOld+2,1.5);

        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType,post.getCreatedAt(),hotScore, post.getMediaUrl(), post.getMediaType());
    }



}
