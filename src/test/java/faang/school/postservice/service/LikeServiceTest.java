package faang.school.postservice.service;

import faang.school.postservice.dto.like.LikeDto;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.repository.LikeRepository;
import faang.school.postservice.repository.PostRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для LikeService")
public class LikeServiceTest {

    private final long userId = 1L;
    private final long postId = 1L;
    private final long commentId = 1L;

    private LikeDto likeDto;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private LikeService likeService;

    @BeforeEach
    public void setup() {
        likeDto = new LikeDto();
    }

    @Nested
    @DisplayName("Позитивные тесты")
    class PositiveTests {

        @Test
        @DisplayName("Должен добавить лайк для поста")
        void shouldAddLikeToPost() {
            likeDto.setUserId(userId);
            likeDto.setPostId(postId);

            Post post = new Post();
            when(postRepository.findById(likeDto.getPostId())).thenReturn(Optional.of(post));
            when(likeRepository
                    .findByPostIdAndUserId(likeDto.getPostId(), likeDto.getUserId()))
                    .thenReturn(Optional.empty()
            );

            likeService.addLikeToPost(likeDto);

            verify(likeRepository).save(any());
            verify(postRepository).findById(likeDto.getPostId());
        }

        @Test
        @DisplayName("Должен добавить лайк для комментария")
        void shouldAddLikeToComment() {
            likeDto.setUserId(userId);
            likeDto.setCommentId(commentId);

            Comment comment = new Comment();
            when(commentRepository.findById(likeDto.getCommentId())).thenReturn(Optional.of(comment));
            when(likeRepository.findByCommentIdAndUserId(likeDto.getCommentId(), likeDto.getUserId()))
                    .thenReturn(Optional.empty());

            likeService.addLikeToComment(likeDto);

            verify(likeRepository).save(any());
            verify(commentRepository).findById(likeDto.getCommentId());
        }

        @Test
        @DisplayName("Должен удалить лайк с поста")
        void shouldRemoveLikeFromPost() {
            likeDto.setUserId(userId);
            likeDto.setPostId(postId);

            likeService.removeLike(likeDto);

            verify(likeRepository).deleteByPostIdAndUserId(likeDto.getPostId(), likeDto.getUserId());
        }

        @Test
        @DisplayName("Должен удалить лайк с комментария")
        void shouldRemoveLikeFromComment() {
            likeDto.setUserId(userId);
            likeDto.setCommentId(commentId);

            likeService.removeLike(likeDto);

            verify(likeRepository).deleteByCommentIdAndUserId(likeDto.getCommentId(), likeDto.getUserId());
        }
    }

    @Nested
    @DisplayName("Негативные тесты")
    class NegativeTests {

        @Test
        @DisplayName("Должен выбросить исключение, если пост не найден при добавлении лайка")
        void shouldThrowExceptionWhenPostNotFound() {
            likeDto.setUserId(userId);
            likeDto.setPostId(postId);

            when(postRepository.findById(likeDto.getPostId())).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> likeService.addLikeToPost(likeDto));

            verify(postRepository).findById(likeDto.getPostId());
            verify(likeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Должен выбросить исключение, если комментарий не найден при добавлении лайка")
        void shouldThrowExceptionWhenCommentNotFound() {
            likeDto.setUserId(userId);
            likeDto.setCommentId(commentId);

            when(commentRepository.findById(likeDto.getCommentId())).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> likeService.addLikeToComment(likeDto));

            verify(commentRepository).findById(likeDto.getCommentId());
            verify(likeRepository, never()).save(any());
        }
    }
}
