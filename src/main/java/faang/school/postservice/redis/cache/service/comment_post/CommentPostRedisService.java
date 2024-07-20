package faang.school.postservice.redis.cache.service.comment_post;

import faang.school.postservice.redis.cache.entity.CommentRedisCache;

public interface CommentPostRedisService {

    void tryDeleteCommentFromPost(CommentRedisCache comment);

    void tryAddCommentToPost(CommentRedisCache comment);
}
