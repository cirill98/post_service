package faang.school.postservice.mapper;

import faang.school.postservice.dto.CommentDto;
import faang.school.postservice.dto.CommentEditDto;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CommentMapperTest {
    @Spy
    CommentMapperImpl commentMapper;
    private Comment comment;
    private Post post;
    private Comment commentUpdated;
    private CommentDto commentDto;
    List<Comment> comments;
    List<CommentDto> commentsDto;
    CommentEditDto commentEditDto;

    @BeforeEach
    void setUp() {
        post = Post.builder().id(1L).build();
        comment = Comment.builder().id(1L).authorId(1L).post(post).content("comment").build();
        commentDto = CommentDto.builder().id(1L).authorId(1L).postId(1L).content("comment").build();
        comments = List.of(comment);
        commentsDto = List.of(commentDto);
    }

    @Test
    void testToEntity() {
        assertEquals(comment, commentMapper.toEntity(commentDto));
    }

    @Test
    void testToDto() {
        assertEquals(commentDto, commentMapper.toDto(comment));
    }

    @Test
    void testToDtoList() {
        assertEquals(commentsDto, commentMapper.toDtoList(comments));
    }
}