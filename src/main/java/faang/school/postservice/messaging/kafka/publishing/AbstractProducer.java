package faang.school.postservice.messaging.kafka.publishing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractProducer<T> {

    protected final KafkaTemplate<String, Object> kafkaTemplate;

    protected ThreadLocal<Integer> retryCount = ThreadLocal.withInitial(() -> 0);

    @Value("${spring.kafka.max-retries}")
    protected int maxRetries;

    public abstract void publish(T event);

    protected void retryPublish(T event) {
        if (retryCount.get() < maxRetries) {
            retryCount.set(retryCount.get() + 1);
            log.info("Retrying... Attempt {}/{}", retryCount, maxRetries);
            publish(event);
        } else {
            log.error("Max retry attempts reached. Failed to publish event: {}", event);
            retryCount.set(0);
        }
    }
}

