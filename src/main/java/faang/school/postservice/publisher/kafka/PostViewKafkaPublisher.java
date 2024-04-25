package faang.school.postservice.publisher.kafka;

import faang.school.postservice.dto.kafka.PostViewedEventDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostViewKafkaPublisher extends AbstractKafkaPublisher<PostViewedEventDto> {
    @Value("${kafka.topics.post_view.name}")
    private String postViewTopic;

    public PostViewKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        super(kafkaTemplate);
    }

    public void publish(long postId, long ownerId) {
        send(postViewTopic, PostViewedEventDto.builder()
                .postId(postId)
                .viewerId(ownerId)
                .build());
    }
}
