package faang.school.postservice.service.post;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.config.moderation.ModerationDictionary;
import faang.school.postservice.entity.dto.post.PostCreateDto;
import faang.school.postservice.entity.dto.post.PostDto;
import faang.school.postservice.entity.dto.post.PostHashtagDto;
import faang.school.postservice.entity.dto.post.PostUpdateDto;
import faang.school.postservice.event.post.PostViewEvent;
import faang.school.postservice.exception.NotFoundException;
import faang.school.postservice.mapper.PostMapper;
import faang.school.postservice.entity.model.Post;
import faang.school.postservice.entity.model.VerificationStatus;
import faang.school.postservice.kafka.producer.PostViewProducer;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.hashtag.async.AsyncHashtagService;
import faang.school.postservice.service.kafka.KafkaPostService;
import faang.school.postservice.service.redis.CachedEntityBuilder;
import faang.school.postservice.service.spelling.SpellingService;
import faang.school.postservice.validator.post.PostValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostValidator postValidator;
    private final AsyncHashtagService asyncHashtagService;
    private final ModerationDictionary moderationDictionary;
    private final SpellingService spellingService;
    private final PostViewProducer postViewPublisher;
    private final KafkaPostService kafkaPostService;
    private final UserContext userContext;
    private final CachedEntityBuilder cachedEntity;

    @Override
    public PostDto getById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Post with id %s not found", id)));
        PostDto dto = postMapper.toDto(post);
//        publishPostViewEvent(dto); //TODO
        return dto;
    }

    @Override
    @Transactional
    public PostDto create(PostCreateDto postCreateDto) {
        postValidator.validateAuthor(postCreateDto.getAuthorId(), postCreateDto.getProjectId());
        postValidator.validatePostContent(postCreateDto.getContent());
        Post post = postRepository.save(postMapper.toEntity(postCreateDto));
        return postMapper.toDto(post);
    }

    @Override
    @Transactional
    public PostDto publish(Long id) {
        Post post = findPostByIdInDB(id);
        postValidator.validatePublicationPost(post);
        post.setPublished(true);
        post.setPublishedAt(LocalDateTime.now());
        post = postRepository.save(post);

        PostHashtagDto postHashtagDto = postMapper.toHashtagDto(post);
        asyncHashtagService.addHashtags(postHashtagDto);

        PostDto dto = postMapper.toDto(post);

        cachedEntity.buildAndSaveNewRedisUser(dto.getAuthorId());
        cachedEntity.buildAndSaveNewRedisPost(dto.getId());

        kafkaPostService.sendPostToPublisher(dto);

        return dto;
    }

    @Override
    @Transactional
    public PostDto update(Long id, PostUpdateDto postUpdateDto) {
        Post post = findPostByIdInDB(id);
        postValidator.validatePostContent(post.getContent());
        post.setContent(postUpdateDto.getContent());
        post = postRepository.save(post);

        PostHashtagDto postHashtagDto = postMapper.toHashtagDto(post);
        asyncHashtagService.updateHashtags(postHashtagDto);

        return postMapper.toDto(post);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Post post = findPostByIdInDB(id);
        post.setDeleted(true);
        postRepository.save(post);

        PostHashtagDto postHashtagDto = postMapper.toHashtagDto(post);
        asyncHashtagService.removeHashtags(postHashtagDto);
    }

    @Override
    public List<PostDto> findAllByHashtag(String hashtag, Pageable pageable) {
        return asyncHashtagService.getPostsByHashtag(hashtag, pageable).join().stream()
                .map(postMapper::toDto)
                .toList();
    }

    @Override
    public List<PostDto> findPostDraftsByUserAuthorId(Long id) {
        return postRepository.findByAuthorIdAndPublishedAndDeletedWithLikes(id, false, false).stream()
                .map(postMapper::toDto)
//                .sorted(Comparator.comparing(PostDto::getCreatedAt).reversed()) TODO
                .toList();
    }

    @Override
    public List<PostDto> findPostDraftsByProjectAuthorId(Long id) {
        return postRepository.findByProjectIdAndPublishedAndDeletedWithLikes(id, false, false).stream()
                .map(postMapper::toDto)
//                .sorted(Comparator.comparing(PostDto::getCreatedAt).reversed()) TODO
                .toList();
    }

    @Override
    public List<PostDto> findPostPublicationsByUserAuthorId(Long id) {
        return postRepository.findByAuthorIdAndPublishedAndDeletedWithLikes(id, true, false).stream()
                .map(postMapper::toDto)
                .peek(this::publishPostViewEvent)
//                .sorted(Comparator.comparing(PostDto::getPublishedAt).reversed()) TODO
                .toList();
    }

    @Override
    public List<PostDto> findPostPublicationsByProjectAuthorId(Long id) {
        return postRepository.findByProjectIdAndPublishedAndDeletedWithLikes(id, true, false).stream()
                .map(postMapper::toDto)
                .peek(this::publishPostViewEvent)
//                .sorted(Comparator.comparing(PostDto::getPublishedAt).reversed()) TODO
                .toList();
    }

    @Override
    @Async("executorService")
    public void verifyPost(List<Post> posts) {
        for (Post post : posts) {

            if (moderationDictionary.checkCurseWordsInPost(post.getContent())) {
                post.setIsVerify(VerificationStatus.NOT_VERIFIED);
            } else {
                post.setIsVerify(VerificationStatus.VERIFIED);
            }

            post.setVerifiedDate(LocalDateTime.now());
            postRepository.save(post);
        }
    }

    @Override
    public void correctPosts() {
        List<Post> unpublishedPosts = postRepository.findReadyToPublish();
        Map<Post, CompletableFuture<Optional<String>>> correctedContents = new HashMap<>();
        unpublishedPosts.stream()
                .filter(post -> !post.isCheckedForSpelling())
                .forEach(post -> correctedContents.put(post, spellingService.checkSpelling(post.getContent())));

        correctedContents.forEach((post, correctedContent) -> {
            try {
                correctedContent.get().ifPresent((content) -> {
                    post.setContent(content);
                    post.setCheckedForSpelling(true);
                });
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error when updating a post with an id: {}", post.getId(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private void publishPostViewEvent(PostDto dto) {
        PostViewEvent event = new PostViewEvent(dto, userContext.getUserId(), LocalDateTime.now());
        postViewPublisher.publish(event);
    }

    private Post findPostByIdInDB(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Post with id %s not found", id)));
        return post;
    }
}
