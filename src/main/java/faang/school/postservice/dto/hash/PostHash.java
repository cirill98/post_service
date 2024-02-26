package faang.school.postservice.dto.hash;

import faang.school.postservice.dto.LikePostEvent;
import faang.school.postservice.dto.post.PostViewEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RedisHash(value = "post")
public class PostHash {
    @Id
    private Long postId;
    private Long userAuthorId;
    private Long projectAuthorId;
    private String content;
    private LocalDateTime publishedAt;
    private Set<LikePostEvent> likes = new LinkedHashSet<>();
    private Set<PostViewEvent> views = new LinkedHashSet<>();

    @TimeToLive
    private Long ttl;

    @Version
    private Long version;
}
