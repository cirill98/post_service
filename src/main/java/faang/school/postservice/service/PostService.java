package faang.school.postservice.service;

import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.LikeRepository;
import faang.school.postservice.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private LikeRepository likeRepository;

  public List<PostDto> getPostsByAuthorWithLikes(long authorId) {
    List<Post> posts = postRepository.findByAuthorIdWithLikes(authorId);
    return posts.stream()
        .map(post -> {
          int likeCount = likeRepository.findByPostId(post.getId()).size();
          return mapToPostDto(post, likeCount);
        })
        .collect(Collectors.toList());
  }

  public List<PostDto> getPostsByProjectWithLikes(long projectId) {
    List<Post> posts = postRepository.findByProjectIdWithLikes(projectId);
    return posts.stream()
        .map(post -> {
          int likeCount = likeRepository.findByPostId(post.getId()).size();
          return mapToPostDto(post, likeCount);
        })
        .collect(Collectors.toList());
  }

  private PostDto mapToPostDto(Post post, int likeCount) {
    return new PostDto(post.getId(), post.getContent(), post.getAuthorId(), post.getProjectId(), likeCount);
  }
}
