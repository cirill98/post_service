package faang.school.postservice.service;

import faang.school.postservice.dto.ResourceDto;
import faang.school.postservice.mapper.ResourceMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.Resource;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.repository.ResourceRepository;
import faang.school.postservice.validator.PostValidator;
import faang.school.postservice.validator.ResourceValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final AmazonS3Service amazonService;
    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;
    private final PostValidator postValidator;
    private final ResourceValidator resourceValidator;
    private final PostRepository postRepository;

//    @Transactional
//    public List<ResourceDto> deleteResources(List<Long> resourceIds) {
//        List<Resource> resourcesToDelete = resourceIds.stream()
//                .map(this::validateAccessAndGetResource)
//                .toList();
//
//        resourcesToDelete.forEach(resource -> amazonService.deleteFile(resource.getKey()));
//
//        resourceRepository.deleteAll(resourcesToDelete);
//        return resourcesToDelete.stream()
//                .map(resourceMapper::toDto)
//                .toList();
//    }

    @Transactional
    public List<ResourceDto> deleteResources(long postId, List<Long> resourceIds) {
        Post post = getPostById(postId);
        List<Resource> resourcesToDelete = resourceIds.stream()
                .map(this::getResourceById)
                .toList();

        resourcesToDelete.forEach(resource -> amazonService.deleteFile(resource.getKey()));
        resourceRepository.deleteAll(resourcesToDelete);
        post.getResources().removeAll(resourcesToDelete);
        postRepository.save(post);

        return resourceMapper.toListDto(resourcesToDelete);
    }

    @Transactional(readOnly = true)
    public ResourceDto getResource(long resourceId) {
        Resource resource = validateAccessAndGetResource(resourceId);

        return resourceMapper.toDto(resource);
    }

    @Transactional
    public List<ResourceDto> createResources(Post post, List<MultipartFile> files) {
        postValidator.validateAccessToPost(post.getAuthorId(), post.getProjectId());

        int existFilesAmount = post.getResources().size();
        int newFilesAmount = files.size();
        resourceValidator.validateFilesAmount(existFilesAmount, newFilesAmount);

        List<Resource> resources = new ArrayList<>();
        files.forEach(file -> {
            resourceValidator.validateImageSize(file.getSize());

            Resource resource = amazonService.uploadFile(file, getFolderName(post.getId(), file.getContentType()));
            resource.setPost(post);
            resources.add(resource);
        });

        List<Resource> savedResources = resourceRepository.saveAll(resources);

        return savedResources.stream()
                .map(resourceMapper::toDto)
                .toList();
    }

    @Transactional
    public List<ResourceDto> addResources(Long postId, List<MultipartFile> files) {
        Post post = getPostById(postId);
        String folder = getFolderName(postId, post.getContent());
        List<Resource> resources = new ArrayList<>();
        for (MultipartFile file : files) {
            Resource resource = amazonService.uploadFile(file, folder);
            resource.setPost(post);
            resources.add(resource);
        }

        resourceRepository.saveAll(resources);
        post.getResources().addAll(resources);
        postRepository.save(post);

        return resourceMapper.toListDto(resources);
    }

    @Transactional(readOnly = true)
    public InputStream downloadResource(long resourceId) {
        Resource resource = validateAccessAndGetResource(resourceId);
        return amazonService.downloadFile(resource.getKey());
    }

    @Transactional(readOnly = true)
    public Resource getResourceById(long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Resource with id %s not found", resourceId)));
    }

    private Resource validateAccessAndGetResource(long id) {
        Resource resource = getResourceById(id);
        Post post = resource.getPost();
        postValidator.validateAccessToPost(post.getAuthorId(), post.getProjectId());
        return resource;
    }

    private Post getPostById(Long postId) {
        return postRepository.findById(postId).orElseThrow(
                () -> new EntityNotFoundException("Post not found"));
    }

    private String getFolderName(long postId, String contentType) {
        return String.format("%s-%s", postId, contentType);
    }

}
