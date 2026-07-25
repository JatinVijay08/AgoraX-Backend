package com.jatin.forum.service;

import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.dto.UpdateUsernameRequest;
import com.jatin.forum.dto.UserResponse;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import com.jatin.forum.exception.UserAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class UserService {
    private final UserRepo userRepo;
    private final PostRepo postRepo;
    private final CommentRepo commentRepo;
    private final PostMapper postMapper;
    private final PostVoteRepo postVoteRepo;

    public UserService(UserRepo userRepo, PostRepo postRepo, PostVoteRepo postVoteRepo, CommentRepo commentRepo, PostMapper postMapper) {
        this.userRepo = userRepo;
        this.postRepo = postRepo;
        this.commentRepo = commentRepo;
        this.postMapper = postMapper;
        this.postVoteRepo = postVoteRepo;
    }


    public com.jatin.forum.dto.UserProfileResponse findById(Long id) {
        User user = userRepo.findById(id).orElseThrow(() -> {
            return new RuntimeException("User not found");
        });
        return buildUserProfileResponse(user);
    }

    public com.jatin.forum.dto.UserProfileResponse findByUsernameProfile(String username) {
        User user = userRepo.findByUsername(username).orElseThrow(() -> {
            return new RuntimeException("User not found");
        });
        return buildUserProfileResponse(user);
    }

    private com.jatin.forum.dto.UserProfileResponse buildUserProfileResponse(User user) {
        List<Post> posts = postRepo.getPostByUserId(user.getId());
        long postCount = posts.size();
        long commentCount = 0;
        long karma = 0;
        List<Long> postIds = new ArrayList<>();
        for (Post post : posts) {
            postIds.add(post.getId());
            commentCount += post.getCommentCount();
        }
        
        if (!postIds.isEmpty()) {
            List<Object[]> results = postVoteRepo.getAggregateVotesForPosts(postIds);
            for (Object[] result : results) {
                Number count = (Number) result[1];
                if (count != null) {
                    karma += count.longValue();
                }
            }
        }
        
        return new com.jatin.forum.dto.UserProfileResponse(user.getUsername(), user.getEmail(), user.getCreated(), postCount, commentCount, karma);
    }


    public List<PostResponse> getPostsSorted(Long id, String sort,User user) {
        // removing the query finding VoteType by user and post for each post

        List<Post> posts = postRepo.getPostByUserId(id);
        List<Long> postIds = new ArrayList<>();
        for(Post post:posts){
            postIds.add(post.getId());
        }

        // select PostVote from repo where user = user and postID IN(1,2,3,4,...) -> Ine one operation gets all votetypes
        // for all posts
        List<PostVote> postVotes = postVoteRepo.findByUserAndPostIdIn(user,postIds);
        HashMap<Long, VoteType> voteTypeHashMap = new HashMap<>();

        for(PostVote vote:postVotes){
            voteTypeHashMap.put(vote.getPost().getId(), vote.getVoteType());
        }
        
        HashMap<Long, Long> voteCountMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<Object[]> results = postVoteRepo.getAggregateVotesForPosts(postIds);
            for (Object[] result : results) {
                voteCountMap.put((Long) result[0], ((Number) result[1]).longValue());
            }
        }

        List<PostResponse> postResponses = posts.stream()
                .map(post -> postMapper.mapToPostResponse(post,voteTypeHashMap,voteCountMap))
                .toList();

        if ("top".equals(sort)) {
            return postResponses.stream()
                    .sorted(Comparator.comparingLong(PostResponse::voteCount).reversed())
                    .toList();
        }
        // default: new (already sorted by DB insertion order, but sort by createdAt to be explicit)
        return postResponses.stream()
                .sorted(Comparator.comparing(PostResponse::createdAt).reversed())
                .toList();
    }

    public List<PostResponse> getPostsByUsernameSorted(String username, String sort) {
        User user = userRepo.findByUsername(username).orElseThrow(() -> {
            return new RuntimeException("User not found");
        });
        return getPostsSorted(user.getId(), sort,user);
    }

    @Transactional
    public UserResponse updateUsername(Long userId, UpdateUsernameRequest request) {
        if (userRepo.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username '" + request.username() + "' is already taken");
        }
        User user = userRepo.findById(userId).orElseThrow(() -> {
            return new RuntimeException("User not found");
        });
        user.setUsername(request.username());
        userRepo.save(user);
        return new UserResponse(user.getUsername(), user.getEmail(), user.getCreated());
    }

    public List<UserResponse> getRecentUsers(Long currentUserId) {
        return userRepo.findUserByLastLoginAtBefore(Instant.now()).stream()
                .filter(user -> user.getLastLoginAt() != null)
                .filter(user -> !user.getId().equals(currentUserId))
                .sorted(Comparator.comparing(User::getLastLoginAt).reversed())
                .limit(5)
                .map(user -> new UserResponse(user.getUsername(), user.getEmail(), null))
                .toList();
    }
}

