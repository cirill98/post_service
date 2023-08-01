package faang.school.postservice.service;

import faang.school.postservice.dto.CommentDto;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.mapper.CommentMapperImpl;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.validator.CommentValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @InjectMocks
    private CommentService commentService;
    @Spy
    private CommentMapperImpl commentMapper;
    @Mock
    private CommentValidator commentValidator;
    @Mock
    CommentRepository commentRepository;
    @Mock
    private PostService postService;

    @Test
    void testCreateComment_ValidData_ReturnsCreatedCommentDto() {
        long postId = 1L;

        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .content("Test comment")
                .authorId(1L)
                .build();

        Post post = Post.builder()
                .id(postId)
                .content("Test post content")
                .build();

        Comment comment = Comment.builder()
                .id(1L)
                .content("Test comment")
                .build();

        when(postService.getPostById(postId)).thenReturn(post);
        when(commentMapper.toEntity(any(CommentDto.class))).thenReturn(comment);
        when(commentRepository.save(comment)).thenReturn(comment);

        CommentDto createdCommentDto = commentService.createComment(postId, commentDto);

        verify(commentValidator).validateUserBeforeCreate(commentDto);
        verify(postService).getPostById(postId);
        verify(commentMapper).toEntity(commentDto);
        verify(commentRepository).save(comment);

        assertEquals(commentDto.getId(), createdCommentDto.getId());
        assertEquals(commentDto.getContent(), createdCommentDto.getContent());
    }


    @Test
    void testUpdateComment_ValidData_ReturnsUpdatedCommentDto() {
        long commentId = 1L;
        CommentDto commentDto = CommentDto.builder()
                .id(commentId)
                .content("Updated comment content")
                .authorId(2L)
                .postId(3L)
                .build();

        Comment existingComment = Comment.builder()
                .id(commentId)
                .content("Original comment content")
                .authorId(2L)
                .post(Post.builder().id(3L).build())
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));

        commentService.updateComment(commentId, commentDto);

        verify(commentRepository, times(1)).findById(commentId);
        verify(commentValidator).validateBeforeUpdate(existingComment, commentDto);
        verify(commentMapper).partialUpdate(commentDto, existingComment);
        verify(commentMapper, times(1)).toDto(existingComment);
    }

    @Test
    public void testUpdateComment_CommentNotFound_ThrowsEntityNotFoundException() {
        long commentId = 1L;
        CommentDto commentDto = CommentDto.builder()
                .id(commentId)
                .content("Updated comment content")
                .authorId(2L)
                .postId(3L)
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> commentService.updateComment(commentId, commentDto));

        assertEquals("Comment with id 1 not found", exception.getMessage());
    }

    @Test
    void testGetCommentsByPostId_ValidPostId_ReturnsSortedCommentDtoList() {
        long postId = 1L;

        Comment comment1 = Comment.builder().id(1L)
                .content("Comment 1")
                .createdAt(LocalDateTime.now().minusHours(3)).build();
        Comment comment2 = Comment.builder().id(2L)
                .content("Comment 2")
                .createdAt(LocalDateTime.now().minusHours(2)).build();

        List<Comment> comments = List.of(comment1, comment2);

        CommentDto commentDto1 = CommentDto.builder().id(1L)
                .content("Comment 1").build();
        CommentDto commentDto2 = CommentDto.builder().id(2L)
                .content("Comment 2").build();

        when(commentRepository.findAllByPostIdOrderByCreatedAtDesc(postId)).thenReturn(comments);
        when(commentMapper.toDto(comment1)).thenReturn(commentDto1);
        when(commentMapper.toDto(comment2)).thenReturn(commentDto2);

        List<CommentDto> result = commentService.getCommentsByPostId(postId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(commentDto1, result.get(0));
        assertEquals(commentDto2, result.get(1));

        verify(commentRepository, times(1)).findAllByPostIdOrderByCreatedAtDesc(postId);
        verify(commentMapper, times(1)).toDto(comment1);
        verify(commentMapper, times(1)).toDto(comment2);
    }

    @Test
    void testGetCommentsByPostId_InvalidPostId_ReturnsEmptyList() {
        long postId = 9L;

        when(commentRepository.findAllByPostIdOrderByCreatedAtDesc(postId)).thenReturn(Collections.emptyList());

        List<CommentDto> result = commentService.getCommentsByPostId(postId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(commentRepository, times(1)).findAllByPostIdOrderByCreatedAtDesc(postId);
        verifyNoMoreInteractions(commentMapper);
    }

    @Test
    void testDeleteComment_ExistingCommentId_DeletesComment() {
        long commentId = 1L;
        Comment comment = Comment.builder()
                .id(commentId)
                .content("Comment content")
                .build();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        commentService.deleteComment(commentId);

        verify(commentRepository, times(1)).findById(commentId);

        verify(commentRepository, times(1)).deleteById(commentId);
    }

    @Test
    void testDeleteComment_NonExistentCommentId_ThrowsEntityNotFoundException() {
        long commentId = 9L;

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> commentService.deleteComment(commentId));
        assertEquals("Comment with id " + commentId + " not found", exception.getMessage());

        verify(commentRepository, times(1)).findById(commentId);
        verifyNoMoreInteractions(commentRepository);
    }
}