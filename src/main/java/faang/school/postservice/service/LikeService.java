package faang.school.postservice.service;

import faang.school.postservice.dto.like.LikeDto;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.repository.LikeRepository;
import faang.school.postservice.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

  @Autowired
  private LikeRepository likeRepository;

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private CommentRepository commentRepository;

  public void addLikeToPost(LikeDto likeDto) {
    if (likeDto.getPostId() != null) {
      Post post = postRepository.findById(likeDto.getPostId())
          .orElseThrow(() -> new IllegalArgumentException("Post not found"));

      likeRepository.findByPostIdAndUserId(likeDto.getPostId(), likeDto.getUserId())
          .ifPresent(like -> {
            throw new IllegalArgumentException("Like already exists");
          });

      Like like = Like.builder()
          .userId(likeDto.getUserId())
          .post(post)
          .build();
      likeRepository.save(like);

    }
  }

  public void addLikeToComment(LikeDto likeDto) {
    if (likeDto.getCommentId() != null) {
      Comment comment = commentRepository.findById(likeDto.getCommentId())
          .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

      likeRepository.findByCommentIdAndUserId(likeDto.getCommentId(), likeDto.getUserId())
          .ifPresent(like -> {
            throw new IllegalArgumentException("Like already exists");
          });

      Like like = Like.builder()
          .userId(likeDto.getUserId())
          .comment(comment)
          .build();
      likeRepository.save(like);
    }
  }

  public void removeLike(LikeDto likeDto) {
    if (likeDto.getPostId() != null) {
      likeRepository.deleteByPostIdAndUserId(likeDto.getPostId(), likeDto.getUserId());
    } else if (likeDto.getCommentId() != null) {
      likeRepository.deleteByCommentIdAndUserId(likeDto.getCommentId(), likeDto.getUserId());
    }
  }
}
