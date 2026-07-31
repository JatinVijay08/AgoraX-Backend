package com.jatin.forum.service;

import com.jatin.forum.dto.PostFeedResponse;
import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.User;
import com.jatin.forum.exception.ResourceNotFoundException;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.strategy.FeedStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock private PostRepo postRepo;
    @Mock private PostVoteRepo postVoteRepo;
    @Mock private CommentRepo commentRepo;
    @Mock private FeedCacheService feedCacheService;
    @Mock private CurrentUserService currentUserService;
    @Mock private PostMapper postMapper;
    @Mock private FeedStrategy mockFeedStrategy;

    private PostService postService;
    private User user;
    private Post post;
    private final Long postId = 1L;

    @BeforeEach
    void setUp() {
        Map<String, FeedStrategy> strategyMap = Map.of("new", mockFeedStrategy);
        postService = new PostService(postRepo, postVoteRepo, commentRepo, feedCacheService, currentUserService, postMapper, strategyMap);

        user = new User();
        user.setId(10L);
        user.setUsername("author");
        user.setEmail("author@email.com");

        post = new Post("Test Title", "Test Content", user);
        post.setId(postId);
    }

    @Test
    @DisplayName("1. Strategy Delegation -> Should delegate getAllPosts to matching FeedStrategy")
    void getAllPosts_MatchingStrategy_ShouldDelegateToStrategy() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        PostFeedResponse expectedResponse = new PostFeedResponse(List.of(), null, false);
        when(mockFeedStrategy.fetchFeed(user, 0, 10, null)).thenReturn(expectedResponse);

        PostFeedResponse result = postService.getAllPosts("new", 0, 10, null);

        assertNotNull(result);
        verify(mockFeedStrategy, times(1)).fetchFeed(user, 0, 10, null);
    }

    @Test
    @DisplayName("2. Missing Strategy -> Should return empty feed response when strategy key invalid")
    void getAllPosts_UnknownStrategy_ShouldReturnEmptyFeed() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));

        PostFeedResponse result = postService.getAllPosts("invalid_type", 0, 10, null);

        assertNotNull(result);
        assertTrue(result.posts().isEmpty());
        verifyNoInteractions(mockFeedStrategy);
    }

    @Test
    @DisplayName("3. Create Post -> Saves post and increments feed activity counters")
    void createPost_ValidInput_ShouldSaveAndIncrementActivity() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.save(any(Post.class))).thenReturn(post);
        when(postMapper.mapToPostResponse(eq(post), any(), any()))
                .thenReturn(new PostResponse("author", postId, "Test Title", "Test Content", 0L, 0L, null, null, 0.0, null, null));

        PostResponse response = postService.createPost("Test Title", "Test Content", null, null, null);

        assertNotNull(response);
        verify(postRepo, times(1)).save(any(Post.class));
        verify(feedCacheService, times(1)).incrementActivity(FeedCacheService.TYPE_NEW);
        verify(feedCacheService, times(1)).incrementActivity(FeedCacheService.TYPE_HOT);
        verify(feedCacheService, times(1)).incrementActivity(FeedCacheService.TYPE_TRENDING);
    }

    @Test
    @DisplayName("4. Get Post By Id -> Returns post enriched with user vote state")
    void getPostById_ExistingId_ShouldReturnPostResponse() {
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(postMapper.getVoteCountHashMap(Collections.singletonList(postId))).thenReturn(new HashMap<>());
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postVoteRepo.findByUserAndPost(user, post)).thenReturn(Optional.empty());
        when(postMapper.mapToPostResponse(eq(post), any(), any()))
                .thenReturn(new PostResponse("author", postId, "Test Title", "Test Content", 0L, 0L, null, null, 0.0, null, null));

        PostResponse response = postService.getPostById(postId);

        assertNotNull(response);
        assertEquals(postId, response.id());
    }

    @Test
    @DisplayName("5. Get NonExistent Post -> Should throw ResourceNotFoundException")
    void getPostById_NonExistent_ShouldThrowException() {
        when(postRepo.findById(postId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> postService.getPostById(postId));
    }

    @Test
    @DisplayName("6. Delete Post Authorized Owner -> Deletes post, comments, votes, and evicts cache")
    void deletePostById_AuthorizedOwner_ShouldDeleteAllRelatedData() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));

        postService.deletePostById(postId);

        verify(postVoteRepo, times(1)).deleteByPostId(postId);
        verify(commentRepo, times(1)).deleteByPostId(postId);
        verify(postRepo, times(1)).deleteById(postId);
        verify(feedCacheService, times(1)).evictFeed(FeedCacheService.TYPE_NEW, 10);
    }

    @Test
    @DisplayName("7. Delete Post Unauthorized -> Should throw AccessDeniedException")
    void deletePostById_UnauthorizedUser_ShouldThrowAccessDenied() {
        User anotherUser = new User();
        anotherUser.setId(999L); // Different ID

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(anotherUser));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));

        assertThrows(AccessDeniedException.class, () -> postService.deletePostById(postId));
        verify(postRepo, never()).deleteById(any());
    }
}
