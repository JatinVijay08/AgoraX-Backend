package com.jatin.forum.service;

import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.*;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VoteServiceTest {

    @Mock private PostVoteRepo postVoteRepo;
    @Mock private UserRepo userRepo;
    @Mock private PostRepo postRepo;
    @Mock private PostMapper postMapper;
    @Mock private NotificationService notificationService;
    @Mock private FeedCacheService feedCacheService;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private VoteService voteService;

    private User user;
    private Post post;
    private final Long postId = 1L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setUsername("jatin");
        user.setEmail("jatin@email.com");

        User author = new User();
        author.setId(20L);
        author.setUsername("author");

        post = new Post("Title", "Content", author);
        post.setId(postId);
    }

    @Test
    @DisplayName("1. Upvote First Time -> Should increment upvote count, save vote, and trigger notification")
    void voteOnPost_FirstUpvote_ShouldIncrementAndNotify() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(postVoteRepo.findByUserAndPost(user, post)).thenReturn(Optional.empty());

        voteService.voteOnPost(postId, VoteType.upvote);

        verify(postRepo, times(1)).incrementUpvoteCount(postId);
        verify(postVoteRepo, times(1)).save(any(PostVote.class));
        verify(notificationService, times(1)).createNotification(post, user, NotificationType.POST_LIKE);
        verify(feedCacheService, times(1)).incrementActivity(FeedCacheService.TYPE_HOT);
    }

    @Test
    @DisplayName("2. Downvote First Time -> Should increment downvote count and NOT send like notification")
    void voteOnPost_FirstDownvote_ShouldIncrementDownvoteOnly() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(postVoteRepo.findByUserAndPost(user, post)).thenReturn(Optional.empty());

        voteService.voteOnPost(postId, VoteType.downvote);

        verify(postRepo, times(1)).incrementDownvoteCount(postId);
        verify(postVoteRepo, times(1)).save(any(PostVote.class));
        verify(notificationService, never()).createNotification(any(), any(), any());
    }

    @Test
    @DisplayName("3. Untoggle Upvote -> Clicking upvote again should decrement upvote and delete vote record")
    void voteOnPost_ToggleUpvote_ShouldDecrementAndDelete() {
        PostVote existingUpvote = new PostVote(user, post, VoteType.upvote);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(postVoteRepo.findByUserAndPost(user, post)).thenReturn(Optional.of(existingUpvote));

        voteService.voteOnPost(postId, VoteType.upvote);

        verify(postRepo, times(1)).decrementUpvoteCount(postId);
        verify(postVoteRepo, times(1)).delete(existingUpvote);
    }

    @Test
    @DisplayName("4. Untoggle Downvote -> Clicking downvote again should decrement downvote and delete vote record")
    void voteOnPost_ToggleDownvote_ShouldDecrementAndDelete() {
        PostVote existingDownvote = new PostVote(user, post, VoteType.downvote);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(postVoteRepo.findByUserAndPost(user, post)).thenReturn(Optional.of(existingDownvote));

        voteService.voteOnPost(postId, VoteType.downvote);

        verify(postRepo, times(1)).decrementDownvoteCount(postId);
        verify(postVoteRepo, times(1)).delete(existingDownvote);
    }

    @Test
    @DisplayName("5. Swap Vote Downvote to Upvote -> Should increment upvote, decrement downvote, and send notification")
    void voteOnPost_SwapDownvoteToUpvote_ShouldAtomicSwap() {
        PostVote existingDownvote = new PostVote(user, post, VoteType.downvote);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(postVoteRepo.findByUserAndPost(user, post)).thenReturn(Optional.of(existingDownvote));

        voteService.voteOnPost(postId, VoteType.upvote);

        verify(postRepo, times(1)).incrementUpvoteCount(postId);
        verify(postRepo, times(1)).decrementDownvoteCount(postId);
        verify(postVoteRepo, times(1)).save(existingDownvote);
        assertEquals(VoteType.upvote, existingDownvote.getVoteType());
        verify(notificationService, times(1)).createNotification(post, user, NotificationType.POST_LIKE);
    }

    @Test
    @DisplayName("6. Swap Vote Upvote to Downvote -> Should increment downvote, decrement upvote, and update record")
    void voteOnPost_SwapUpvoteToDownvote_ShouldAtomicSwap() {
        PostVote existingUpvote = new PostVote(user, post, VoteType.upvote);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(postVoteRepo.findByUserAndPost(user, post)).thenReturn(Optional.of(existingUpvote));

        voteService.voteOnPost(postId, VoteType.downvote);

        verify(postRepo, times(1)).incrementDownvoteCount(postId);
        verify(postRepo, times(1)).decrementUpvoteCount(postId);
        verify(postVoteRepo, times(1)).save(existingUpvote);
        assertEquals(VoteType.downvote, existingUpvote.getVoteType());
    }

    @Test
    @DisplayName("7. Activity Threshold Eviction -> Triggers cache eviction when threshold exceeded")
    void voteOnPost_HighActivity_ShouldEvictHotAndTrendingCaches() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(postVoteRepo.findByUserAndPost(user, post)).thenReturn(Optional.empty());

        when(feedCacheService.incrementActivity(FeedCacheService.TYPE_HOT)).thenReturn(15L);
        when(feedCacheService.incrementActivity(FeedCacheService.TYPE_TRENDING)).thenReturn(15L);
        when(feedCacheService.shouldEvict(eq(FeedCacheService.TYPE_HOT), anyLong())).thenReturn(true);
        when(feedCacheService.shouldEvict(eq(FeedCacheService.TYPE_TRENDING), anyLong())).thenReturn(true);

        voteService.voteOnPost(postId, VoteType.upvote);

        verify(feedCacheService, times(1)).evictFeed(FeedCacheService.TYPE_HOT, 10);
        verify(feedCacheService, times(1)).evictFeed(FeedCacheService.TYPE_TRENDING, 10);
    }

    @Test
    @DisplayName("8. User Unauthenticated -> Should throw RuntimeException when user not logged in")
    void voteOnPost_Unauthenticated_ShouldThrowException() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> voteService.voteOnPost(postId, VoteType.upvote));
        verifyNoInteractions(postVoteRepo);
    }
}
