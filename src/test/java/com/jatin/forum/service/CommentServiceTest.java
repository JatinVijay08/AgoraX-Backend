package com.jatin.forum.service;

import com.jatin.forum.dto.CommentResponse;
import com.jatin.forum.dto.CreateCommentRequest;
import com.jatin.forum.entity.Comment;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.User;
import com.jatin.forum.exception.ResourceNotFoundException;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.CommentVoteRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock private PostRepo postRepo;
    @Mock private CommentRepo commentRepo;
    @Mock private CommentVoteRepo commentVoteRepo;
    @Mock private UserRepo userRepo;
    @Mock private FeedCacheService feedCacheService;
    @Mock private CurrentUserService currentUserService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    private User user;
    private User postAuthor;
    private Post post;
    private final Long postId = 1L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(100L);
        user.setUsername("commenter");
        user.setEmail("commenter@email.com");

        postAuthor = new User();
        postAuthor.setId(200L);
        postAuthor.setUsername("author");
        postAuthor.setEmail("author@email.com");

        post = new Post("Post Title", "Post Content", postAuthor);
        post.setId(postId);
    }

    @Test
    @DisplayName("1. Create Root Comment -> Saves comment, increments post comment count, and notifies post author")
    void createComment_RootComment_ShouldSaveAndIncrementCount() {
        CreateCommentRequest request = new CreateCommentRequest("Great post!", null);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.CreateComment(postId, request);

        assertNotNull(response);
        assertEquals("Great post!", response.content());
        verify(commentRepo, times(1)).save(any(Comment.class));
        verify(postRepo, times(1)).incrementCommentCount(postId);
        verify(notificationService, times(1)).createCommentNotification(eq(post), eq(user), any(Comment.class));
    }

    @Test
    @DisplayName("2. Create Reply Comment -> Saves reply comment with parent ID and triggers reply notification")
    void createComment_ReplyToParent_ShouldSaveAndNotifyParentAuthor() {
        User parentAuthor = new User();
        parentAuthor.setId(300L);
        Comment parentComment = new Comment("Parent comment", parentAuthor, post);
        parentComment.setId(50L);

        CreateCommentRequest request = new CreateCommentRequest("I agree!", 50L);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepo.findById(50L)).thenReturn(Optional.of(parentComment));
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.CreateComment(postId, request);

        assertNotNull(response);
        assertEquals(50L, response.parentComment());
        verify(notificationService, times(1)).createReplyNotification(eq(post), eq(user), any(Comment.class), eq(parentComment));
    }

    @Test
    @DisplayName("3. Create Comment NonExistent Post -> Should throw ResourceNotFoundException")
    void createComment_NonExistentPost_ShouldThrowException() {
        CreateCommentRequest request = new CreateCommentRequest("Nice!", null);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.CreateComment(postId, request));
    }

    @Test
    @DisplayName("4. Delete Comment Authorized Owner -> Should delete comment and decrement post comment count")
    void deleteComment_AuthorizedOwner_ShouldDeleteAndDecrementCount() {
        Comment comment = new Comment("Comment to delete", user, post);
        comment.setId(5L);

        when(commentRepo.findById(5L)).thenReturn(Optional.of(comment));
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));

        commentService.deleteComment(5L);

        verify(commentRepo, times(1)).delete(comment);
        verify(postRepo, times(1)).decrementCommentCount(postId);
    }

    @Test
    @DisplayName("5. Delete Comment Unauthorized User -> Should throw AccessDeniedException")
    void deleteComment_UnauthorizedUser_ShouldThrowAccessDenied() {
        User anotherUser = new User();
        anotherUser.setEmail("unauthorized@email.com");

        Comment comment = new Comment("Comment to delete", user, post);
        comment.setId(5L);

        when(commentRepo.findById(5L)).thenReturn(Optional.of(comment));
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(anotherUser));

        assertThrows(AccessDeniedException.class, () -> commentService.deleteComment(5L));
        verify(commentRepo, never()).delete(any());
    }

    @Test
    @DisplayName("6. Get Comments by PostId -> Returns paginated comments enriched with user vote type")
    void getCommentByPostId_ShouldReturnPaginatedComments() {
        Comment c1 = new Comment("Comment 1", user, post);
        c1.setId(101L);
        c1.setUpvotes(5);
        c1.setDownvotes(1);

        Page<Comment> commentPage = new PageImpl<>(List.of(c1));

        when(commentRepo.findByPostId(eq(postId), any(Pageable.class))).thenReturn(commentPage);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));

        Page<CommentResponse> result = commentService.getCommentByPostId(postId, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals(4L, result.getContent().get(0).voteCount());
    }

    @Test
    @DisplayName("7. Activity Threshold Trigger -> Evicts trending feed cache when threshold is hit")
    void createComment_HighActivity_ShouldTriggerFeedEviction() {
        CreateCommentRequest request = new CreateCommentRequest("Trending trigger!", null);

        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(postRepo.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepo.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(feedCacheService.incrementActivity(FeedCacheService.TYPE_TRENDING)).thenReturn(15L);
        when(feedCacheService.shouldEvict(eq(FeedCacheService.TYPE_TRENDING), anyLong())).thenReturn(true);

        commentService.CreateComment(postId, request);

        verify(feedCacheService, times(1)).evictFeed(FeedCacheService.TYPE_TRENDING, 10);
        verify(feedCacheService, times(1)).resetActivity(FeedCacheService.TYPE_TRENDING);
    }
}
