package faang.school.postservice.controller;

import faang.school.postservice.dto.like.LikeDto;
import faang.school.postservice.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
public class LikeController {

  private final LikeService likeService;

  @PostMapping("/post/{postId}")
  public ResponseEntity<?> addLikeToPost(@PathVariable Long postId, @Valid @RequestBody LikeDto likeDto) {
    likeDto.setPostId(postId);
    likeService.addLikeToPost(likeDto);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/post/{postId}")
  public ResponseEntity<?> removeLikeFromPost(@PathVariable Long postId, @Valid @RequestBody LikeDto likeDto) {
    likeDto.setPostId(postId);
    likeService.removeLike(likeDto);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/comment/{commentId}")
  public ResponseEntity<?> addLikeToComment(@PathVariable Long commentId, @Valid @RequestBody LikeDto likeDto) {
    likeDto.setCommentId(commentId);
    likeService.addLikeToComment(likeDto);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/comment/{commentId}")
  public ResponseEntity<?> removeLikeFromComment(@PathVariable Long commentId, @Valid @RequestBody LikeDto likeDto) {
    likeDto.setCommentId(commentId);
    likeService.removeLike(likeDto);
    return ResponseEntity.ok().build();
  }
}
