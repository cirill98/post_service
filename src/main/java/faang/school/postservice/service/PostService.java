package faang.school.postservice.service;

import faang.school.postservice.client.ProjectServiceClient;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.post.CreatePostDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.UpdatePostDto;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.mapper.PostMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.repository.ad.AdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final AdRepository adRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final UserServiceClient userServiceClient;
    private final ProjectServiceClient projectServiceClient;

    @Transactional
    public PostDto createPost(CreatePostDto postDto) {
        Post post = postMapper.toEntity(postDto);
        if (postDto.getAuthorId() != null && postDto.getProjectId() != null) {
            throw new DataValidationException("The author can be either a user or a project");
        }
        if (postDto.getAuthorId() != null && userServiceClient.getUser(postDto.getAuthorId()) == null) {
            throw new DataValidationException("Author must be Existing on the user's system = " + postDto.getAuthorId()
                    + " or project ID now it = " + postDto.getProjectId());
        }
        if (postDto.getProjectId() != null && projectServiceClient.getProject(postDto.getProjectId()) == null) {
            throw new DataValidationException("You must provide an author ID, now this is = " + postDto.getAuthorId()
                    + " or project ID now it = " + postDto.getProjectId());
        }
        post.setDeleted(false);
        post.setPublished(false);
        return postMapper.toDto(postRepository.save(post));
    }

    @Transactional
    public List<PostDto> publishPost() {
        List<Post> readyToPublish = postRepository.findReadyToPublish();
        if (readyToPublish.isEmpty()) {
            return new ArrayList<>();
        }
        for (Post post : readyToPublish) {
            if (post.isPublished()) {
                continue;
            }
            post.setPublished(true);
            post.setPublishedAt(null);
            postRepository.save(post);
        }
        return postMapper.toDtoList(readyToPublish);
    }

    @Transactional
    public PostDto updatePost(UpdatePostDto postDto) {
        Post postInTheDatabase = postRepository.findById(postDto.getId())
                .orElseThrow(() -> new DataValidationException("Update Post not found"));

        postInTheDatabase.setContent(postDto.getContent());
        postInTheDatabase.setUpdatedAt(null);

        postInTheDatabase.setAd(adRepository.findById(postDto.getAdId())
                .orElseThrow(() -> new DataValidationException("Update ad not found")));

        return postMapper.toDto(postRepository.save(postInTheDatabase));
    }

    @Transactional
    public PostDto softDeletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DataValidationException("Delete Post not found"));
        post.setDeleted(true);
        postRepository.save(post);
        return postMapper.toDto(post);
    }

    @Transactional(readOnly = true)
    public PostDto getPostById(Long id) {
        Optional<Post> postById = postRepository.findById(id);
        if (postById.isEmpty()) {
            throw new DataValidationException("'Post not in database' error occurred while fetching post");
        }
        return postMapper.toDto(postById
                .orElseThrow(() -> new DataValidationException("Post not found")));
    }

    @Transactional
    public List<PostDto> getAllPostsByAuthorId(Long authorId) {
        return postRepository.findByAuthorId(authorId).stream()
                .filter(post -> !post.isDeleted() && !post.isPublished())
                .map(postMapper::toDto)
                .sorted(Comparator.comparing(PostDto::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public List<PostDto> getAllPostsByProjectId(Long projectId) {
        return postRepository.findByProjectId(projectId).stream()
                .filter(post -> !post.isDeleted() && !post.isPublished())
                .map(postMapper::toDto)
                .sorted(Comparator.comparing(PostDto::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public List<PostDto> getAllPostsByAuthorIdAndPublished(Long authorId) {
        return postRepository.findByAuthorId(authorId).stream()
                .filter(post -> !post.isDeleted() && post.isPublished())
                .map(postMapper::toDto)
                .sorted(Comparator.comparing(PostDto::getCreatedAt).reversed())
                .toList();
    }

    @Transactional
    public List<PostDto> getAllPostsByProjectIdAndPublished(Long projectId) {
        return postRepository.findByProjectId(projectId).stream()
                .filter(post -> !post.isDeleted() && post.isPublished())
                .map(postMapper::toDto)
                .sorted(Comparator.comparing(PostDto::getCreatedAt).reversed())
                .toList();
    }
}
