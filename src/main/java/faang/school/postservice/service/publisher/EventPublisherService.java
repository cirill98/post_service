package faang.school.postservice.service.publisher;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.event.kafka.LikeKafkaEvent;
import faang.school.postservice.event.kafka.PostKafkaEvent;
import faang.school.postservice.event.kafka.PostViewKafkaEvent;
import faang.school.postservice.event.redis.like.LikeEvent;
import faang.school.postservice.dto.like.LikeDto;
import faang.school.postservice.mapper.comment.CommentMapper;
import faang.school.postservice.mapper.like.LikeEventMapper;
import faang.school.postservice.mapper.post.PostMapper;
import faang.school.postservice.messaging.publisher.kafka.comment.KafkaCommentPublisher;
import faang.school.postservice.messaging.publisher.kafka.like.KafkaLikePublisher;
import faang.school.postservice.messaging.publisher.kafka.post.KafkaPostPublisher;
import faang.school.postservice.messaging.publisher.kafka.post.KafkaPostViewPublisher;
import faang.school.postservice.messaging.publisher.redis.comment.CommentEventPublisher;
import faang.school.postservice.messaging.publisher.redis.like.LikeEventPublisher;
import faang.school.postservice.messaging.publisher.redis.post.PostEventPublisher;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisherService {
    private final LikeEventMapper likeEventMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final LikeEventPublisher likeEventPublisher;
    private final PostEventPublisher postEventPublisher;
    private final CommentEventPublisher commentEventPublisher;
    private final KafkaPostPublisher kafkaPostPublisher;
    private final KafkaPostViewPublisher kafkaPostViewPublisher;
    private final KafkaLikePublisher kafkaLikePublisher;
    private final KafkaCommentPublisher kafkaCommentPublisher;

    public void submitLikeEventToRedis(LikeDto likeDto) {
        LikeEvent likeEvent = likeEventMapper.toLikeEvent(likeDto);
        likeEventPublisher.publish(likeEvent);
    }
    public void submitPostEventToRedis(Post post){
        postEventPublisher.publish(postMapper.toPostEvent(post));
    }

    public void submitCommentEventToRedis(Comment comment){
        commentEventPublisher.publish(commentMapper.toEvent(comment));
    }

    public void submitPostEventToKafka(UserDto userDto){
        kafkaPostPublisher.publish(PostKafkaEvent.builder()
                .postId(userDto.getId())
                .followers(userDto.getFollowersId())
                .build());
    }

    public void submitPostViewEventToKafka(long postId){
        kafkaPostViewPublisher.publish(PostViewKafkaEvent.builder()
                .postId(postId)
                .build());
    }

    public void submitLikeEventToKafka(Like like){
        kafkaLikePublisher.publish(LikeKafkaEvent.builder()
                .authorId(like.getUserId())
                .commentId(like.getComment().getId())
                .postId(like.getPost().getId())
                .build());
    }

    public void submitCommentEventToKafka(CommentDto commentDto){
        kafkaCommentPublisher.publish(commentMapper.toCommentKafkaEvent(commentDto));
    }
}