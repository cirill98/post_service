package faang.school.postservice.controller;

import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.service.PostService;
import faang.school.postservice.util.validator.PostControllerValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostControllerValidator validator;

    @PostMapping("/add")
    ResponseEntity<PostDto> addPost(@RequestBody PostDto dto) {
        validator.validatePost(dto);

        return ResponseEntity.ok(postService.addPost(dto));
    }
}

