package faang.school.postservice.mapper.feed;

import faang.school.postservice.dto.post.PostFeedDto;
import faang.school.postservice.mapper.comment.CommentMapper;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.redis.PostRedis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CommentMapper.class})
public interface FeedMapper {

    @Mapping(source = "comments", target = "commentRedis")
    PostFeedDto postRedisToPostFeedDto(PostRedis postRedis);

    @Mapping(source = "likes", target = "likes", qualifiedByName = "listLikeToCount")
    @Mapping(source = "comments", target = "commentRedis", qualifiedByName = "mapComments")
    PostFeedDto postToPostFeedDto(Post post);

    @Named("listLikeToCount")
    default long listLikeToCount(List<Like> likes) {
        return likes.size();
    }
}
