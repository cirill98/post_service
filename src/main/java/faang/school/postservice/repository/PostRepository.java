package faang.school.postservice.repository;

import faang.school.postservice.dto.post.PostForFeedHeater;
import faang.school.postservice.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends CrudRepository<Post, Long> {

    List<Post> findByAuthorId(long authorId);

    List<Post> findByProjectId(long projectId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.projectId = :projectId")
    List<Post> findByProjectIdWithLikes(long projectId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.authorId = :authorId")
    List<Post> findByAuthorIdWithLikes(long authorId);

    @Query("SELECT p FROM Post p WHERE p.published = false AND p.deleted = false AND p.scheduledAt <= CURRENT_TIMESTAMP")
    List<Post> findReadyToPublish();

    @Query("SELECT p FROM Post p WHERE p.published = true AND p.deleted = false ORDER BY p.publishedAt DESC")
    List<Post> findNewPostsForHeat(Pageable pageable);

    @Query("""
            SELECT new faang.school.postservice.dto.post.PostForFeedHeater(p.id, p.authorId)
            FROM Post p
            WHERE p.published = true
            AND p.deleted = false
            """)
    List<PostForFeedHeater> findAllPublishedPostsForFeedHeater();

    @Query(value = """
            SELECT * FROM post p
            WHERE p.author_id IN (
                SELECT s.followee_id FROM subscription s
                WHERE s.follower_id = :followerId)
            AND p.published IS TRUE
            AND p.deleted IS FALSE
            AND p.updated_at >= :start
            ORDER BY p.updated_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Post> getPostsByFollowerIdAndTime(@Param("followerId") long followerId,
                                           @Param("limit") int limit,
                                           @Param("start") LocalDateTime start);
}
