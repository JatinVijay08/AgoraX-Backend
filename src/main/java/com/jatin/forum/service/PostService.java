package com.jatin.forum.service;

import com.jatin.forum.dto.CreatePostRequest;
import com.jatin.forum.dto.PostResponse;
import com.jatin.forum.entity.Post;
import com.jatin.forum.entity.PostVote;
import com.jatin.forum.entity.User;
import com.jatin.forum.entity.VoteType;
import com.jatin.forum.repository.CommentRepo;
import com.jatin.forum.repository.PostRepo;
import com.jatin.forum.repository.PostVoteRepo;
import com.jatin.forum.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
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


    public List<PostResponse> getAllPosts(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = null;
        User user = null;

        if(authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")){
           email = authentication.getName();
            user = userRepo.findByEmail(email);
        }
        User finalUser = user;
        return postRepo.findAll()
               .stream()
               .map(post->{
                   long upvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.upvote);
                   long downvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.downvote);

                   VoteType voteType = null;
                   if(finalUser!=null) {
                       voteType = postVoteRepo.findByUserAndPost(finalUser, post).map(PostVote::getVoteType).orElse(null);
                   }

                   long votes = upvotes-downvotes;
                   long commentCount = commentRepo.countByPostId(post.getId());
                   User user1 = post.getUser();
                   return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType,post.getCreatedAt());
               })
               .toList();

    }

    public PostResponse createPost(CreatePostRequest createPostRequest){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepo.findByEmail(email);
        if(user==null){
            throw new RuntimeException("User not found");
        }
        Post post = new Post(createPostRequest.title(), createPostRequest.content(), user);

        Post savedPost = postRepo.save(post);
        return mapToPostResponse(savedPost);
    }

    public PostResponse getPostById(Long id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = null;
        User user = null;
        if(authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")){
             email = authentication.getName();
             user = userRepo.findByEmail(email);
        }

        Post post  = postRepo.findById(id).orElseThrow(()->new RuntimeException("post not found")) ;
        long upvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.upvote);
        long downvotes = postVoteRepo.countByPostAndVoteType(post, VoteType.downvote);
        VoteType voteType = null;

        if(user!=null) {
            voteType = postVoteRepo.findByUserAndPost(user, post).map(PostVote::getVoteType).orElse(null);
        }
         long votes = upvotes-downvotes;
        long commentCount = commentRepo.countByPostId(post.getId());
        User user1 = post.getUser();
        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType,post.getCreatedAt());

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
        return new PostResponse(user1.getUsername(),post.getId(), post.getTitle(), post.getContent(), votes,commentCount, voteType,post.getCreatedAt());

    }



}
