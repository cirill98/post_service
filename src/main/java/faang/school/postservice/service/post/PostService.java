package faang.school.postservice.service.post;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.exception.PostValidationException;
import faang.school.postservice.mapper.post.PostMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.redis.PostRedis;
import faang.school.postservice.model.redis.UserRedis;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.repository.redis.RedisPostRepository;
import faang.school.postservice.repository.redis.RedisUserRepository;
import faang.school.postservice.service.publisher.EventPublisherService;
import faang.school.postservice.validator.post.PostValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostValidator postValidator;
    private final RedisPostRepository redisPostRepository;
    private final RedisUserRepository redisUserRepository;
    private final UserServiceClient userServiceClient;
    private final EventPublisherService eventPublisherService;

    public PostDto create(PostDto postDto) {
        postValidator.validateCreate(postDto);
        boolean authorExists = postValidator.checkIfAuthorExists(postDto);
        if (!authorExists) {
            throw new PostValidationException("Author doesn't exists on system!");
        }

        Post post = postMapper.toEntity(postDto);
        post.setPublished(false);
        post.setDeleted(false);
        return postMapper.toDto(postRepository.save(post));
    }

    public PostDto publish(Long postId) {
        Optional<Post> postOptional = postRepository.findById(postId);
        postValidator.validatePublish(postOptional);

        Post post = postOptional.get();
        post.setPublished(true);
        post.setPublishedAt(LocalDateTime.now());
        eventPublisherService.submitPostEventToRedis(post);

        Post savePost = postRepository.save(post);

        // Сохраняем в редис
        PostRedis postRedis = postMapper.toPostRedis(savePost);
        UserDto userDto = userServiceClient.getUser(postRedis.getAuthorId());
        redisUserRepository.save(UserRedis.builder()
                .id(userDto.getId())
                .username(userDto.getUsername())
                .build());
        log.info("Save user with ID: {} to Redis", userDto.getId());
        redisPostRepository.save(postRedis);
        log.info("Save post with ID: {} to Redis", postRedis.getId());
        eventPublisherService.submitPostEventToKafka(userDto);
        log.info("Send event with Post ID: {} to Kafka", savePost.getId());

        return postMapper.toDto(savePost);
    }

    public PostDto update(Long postId, PostDto postDto) {
        postValidator.validateUpdate(postId, postDto);

        Post post = postMapper.toEntity(postDto);
        post.setUpdatedAt(LocalDateTime.now());

        return postMapper.toDto(postRepository.save(post));
    }

    public PostDto softDelete(Long postId) {
        Optional<Post> postOptional = postRepository.findById(postId);

        Post post = postOptional.orElseThrow(
                () -> new PostValidationException("Post with id " + postId + " doesn't exists"));

        post.setPublished(false);
        post.setDeleted(true);

        return postMapper.toDto(postRepository.save(post));
    }

    public PostDto getById(Long postId) {
        Optional<Post> postOptional = postRepository.findById(postId);
        Post post = postOptional.orElseThrow(
                () -> new PostValidationException("Post with id " + postId + " doesn't exists"));
        eventPublisherService.submitPostEventToRedis(post);
        eventPublisherService.submitPostViewEventToKafka(post.getId());
        return postMapper.toDto(post);
    }

    public List<PostDto> getAllUnpublishedPostsForAuthor(Long authorId) {
        List<Post> posts = postRepository.findByAuthorId(authorId);
        if (posts != null) {
            return getSortedPostsByFilter(posts,
                    post -> !post.isPublished() && !post.isDeleted(),
                    Comparator.comparing(Post::getCreatedAt).reversed());
        }

        throw new PostValidationException("No posts for author with id " + authorId);
    }

    public List<PostDto> getAllUnpublishedPostsForProject(Long projectId) {
        List<Post> posts = postRepository.findByProjectId(projectId);
        if (posts != null) {
            return getSortedPostsByFilter(posts,
                    post -> !post.isPublished() && !post.isDeleted(),
                    Comparator.comparing(Post::getCreatedAt).reversed());
        }

        throw new PostValidationException("No posts for author with id " + projectId);
    }

    public List<PostDto> getAllPublishedPostsForAuthor(Long authorId) {
        List<Post> posts = postRepository.findByAuthorId(authorId);

        if (posts != null) {
            return getSortedPostsByFilter(posts,
                    post -> post.isPublished() && !post.isDeleted(),
                    Comparator.comparing(Post::getPublishedAt).reversed());
        }

        throw new PostValidationException("No posts for author with id " + authorId);
    }

    public List<PostDto> getAllPublishedPostsForProject(Long projectId) {
        List<Post> posts = postRepository.findByProjectId(projectId);

        if (posts != null) {
            return getSortedPostsByFilter(posts,
                    post -> post.isPublished() && !post.isDeleted(),
                    Comparator.comparing(Post::getPublishedAt).reversed());
        }

        throw new PostValidationException("No posts for author with id " + projectId);
    }

    private List<PostDto> getSortedPostsByFilter(List<Post> posts,
                                                 Predicate<Post> predicate, Comparator<Post> comparator) {
        return posts.stream()
                .filter(predicate)
                .sorted(comparator)
                .map(postMapper::toDto)
                .toList();
    }
}