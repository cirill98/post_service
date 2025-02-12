package faang.school.postservice.messaging.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractEventPublisherTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ChannelTopic channelTopic;

    @InjectMocks
    private TestEventPublisher eventPublisher;

    @Test
    void testPublishAccess() throws JsonProcessingException {
        Object event = new Object();
        String message = "test message";

        when(objectMapper.writeValueAsString(event)).thenReturn(message);
        when(redisTemplate.convertAndSend(channelTopic.getTopic(), message)).thenReturn(Long.MIN_VALUE);

        eventPublisher.publish(event);

        verify(objectMapper, times(1)).writeValueAsString(event);
        verify(redisTemplate, times(1)).convertAndSend(channelTopic.getTopic(), message);
    }
}