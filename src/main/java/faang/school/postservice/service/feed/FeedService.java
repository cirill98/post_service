package faang.school.postservice.service.feed;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.post.PostFeedDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.mapper.feed.FeedMapper;
import faang.school.postservice.mapper.post.PostMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.redis.PostRedis;
import faang.school.postservice.model.redis.UserRedis;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.repository.redis.RedisFeedRepository;
import faang.school.postservice.repository.redis.RedisPostRepository;
import faang.school.postservice.repository.redis.RedisUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final RedisFeedRepository redisFeedRepository;
    private final RedisPostRepository redisPostRepository;
    private final RedisUserRepository redisUserRepository;
    private final UserServiceClient userServiceClient;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final FeedMapper feedMapper;

    @Value("${spring.data.redis.cache.capacity.max.news_feed}")
    private int limitNewsFeed;

    public List<PostFeedDto> getNewsFeed(Long postId, long userId) {

        if (postId == null) {
            try {
                return getLimitNewsFeedFromRedis(userId);
            } catch (Exception e) {
                return getLimitNewsFeedFromDB(userId);
            }
        } else {
            return List.of(getOneNewsFromRedis(postId));
        }
    }

    private List<PostFeedDto> getLimitNewsFeedFromRedis(long userId) {
        List<PostRedis> postsRedis = getAllPostRedisByUserId(userId);

        List<Long> userIds = postsRedis.stream().map(PostRedis::getAuthorId).toList();
        Map<Long, UserRedis> userRedisMap = StreamSupport.stream(redisUserRepository.findAllById(userIds).spliterator(), false)
                .collect(Collectors.toMap(UserRedis::getId, user -> user));
        log.info("All authors posts: {}", userRedisMap.values());

        return postsRedis.stream().map(postRedis -> {
            PostFeedDto postFeedDto = feedMapper.postRedisToPostFeedDto(postRedis);
            String userName = userRedisMap.get(postFeedDto.getAuthorId()).getUsername();
            postFeedDto.setAuthorName(userName);
            return postFeedDto;
        }).toList();
    }

    private List<PostRedis> getAllPostRedisByUserId(long userId) {
        TreeSet<Long> postsId = redisFeedRepository.getPostsIdsByFollowerId(userId, limitNewsFeed).orElse(null);
        log.info("Post IDs: {} by user ID: {}", postsId, userId);

        List<PostRedis> postRedis = StreamSupport.stream(redisPostRepository.findAllById(postsId).spliterator(), false).toList();
        log.info("List PostRedis in Redis: {}", postRedis);

        if (postRedis.size() < limitNewsFeed) {
            log.info("The post size: {} of the rices is smaller than the limit: {}", postRedis.size(), limitNewsFeed);
            PostRedis postRedisMin = postRedis.stream()
                    .min(Comparator.comparing(PostRedis::getUpdatedAt)).orElse(null);
            log.info("Post with earliest update date: {}", postRedisMin);
            List<Post> posts = postRepository.getPostsByFollowerIdAndTime(userId, limitNewsFeed - postRedis.size(), postRedisMin.getUpdatedAt());
            postRedis.addAll(posts.stream().map(postMapper::heaterPostToPostRedis).toList());
        }

        return postRedis;
    }

    private List<PostFeedDto> getLimitNewsFeedFromDB(long userId) {
        List<Post> posts = postRepository.getPostsByFollowerIdAndTime(userId, limitNewsFeed, LocalDateTime.now());

        List<Long> authorsId = posts.stream().map(Post::getAuthorId).toList();
        Map<Long, UserRedis> allUsersInDB = userServiceClient.getUsersByIds(authorsId).stream()
                .collect(Collectors.toMap(UserDto::getId,
                        user -> UserRedis.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .build()));

        return posts.stream().map(post -> {
            PostFeedDto postFeedDto = feedMapper.postToPostFeedDto(post);
            String userName = allUsersInDB.get(postFeedDto.getAuthorId()).getUsername();
            postFeedDto.setAuthorName(userName);
            return postFeedDto;
        }).toList();
    }

    private PostFeedDto getOneNewsFromRedis(long postId) {
        return redisPostRepository.findById(postId).flatMap(postRedis ->
                redisUserRepository.findById(postRedis.getAuthorId()).map(userRedis -> {
                    PostFeedDto postFeedDto = feedMapper.postRedisToPostFeedDto(postRedis);
                    postFeedDto.setAuthorName(userRedis.getUsername());
                    return postFeedDto;
                })
        ).orElseGet(() -> {
            log.info("Post by ID: {} or user by author ID not found", postId);
            return null;
        });
    }
}
