package faang.school.postservice.service.comment;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.service.post.PostService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserServiceClient userServiceClient;
    private final PostService postService;
    private final CommentMapper commentMapper;

    public CommentDto addNewCommentInPost(CommentDto commentDto) {
        validateAuthorExists(commentDto.getAuthorId());
        Comment comment = commentMapper.toEntity(commentDto);
        comment.setPost(postService.getPost(commentDto.getPostId()));
        comment.setLikes(new ArrayList<>());
        commentRepository.save(comment);
        return commentMapper.toDto(comment);
    }

    public CommentDto updateExistingComment(CommentDto commentDto) {
        validateAuthorExists(commentDto.getAuthorId());
        Comment comment = commentRepository.findAllByPostId(commentDto.getPostId()).stream()
                .filter(commentOne -> commentOne.getId() == commentDto.getId())
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Комментария с id: {} нет!", commentDto.getPostId());
                    return new IllegalArgumentException("Комментария с таким id нет!");
                });
        comment.setContent(commentDto.getContent());
        commentRepository.save(comment);
        return commentMapper.toDto(comment);
    }

    public List<CommentDto> getCommentsForPost(Long postId) {
        return postService.getPost(postId).getComments().stream()
                .map(commentMapper::toDto)
                .toList();
    }

    public CommentDto deleteExistingCommentInPost(CommentDto commentDto) {
        Comment comments = findCommentInPost(commentDto);
        commentRepository.deleteById(comments.getId());
        return commentMapper.toDto(comments);
    }

    private Comment findCommentInPost(CommentDto commentDto) {
        return commentRepository.findAllByPostId(commentDto.getPostId()).stream()
                .filter(comment ->
                        comment.getAuthorId() == commentDto.getAuthorId() &&
                                comment.getContent().equals(commentDto.getContent()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Комментарий с таким содержанием: {} и id автора: {} не найден",
                            commentDto.getContent(), commentDto.getAuthorId());
                    return new IllegalArgumentException("Комментарий с таким содержанием и автором не найден");
                });
    }

    private void validateAuthorExists(long authorId) {
        UserDto user = userServiceClient.getUser(authorId);
        if (user == null) {
            log.error("Пользователь с таким ID не найден");
            throw new EntityNotFoundException("Пользователь с таким ID не найден");
        }
    }
}
