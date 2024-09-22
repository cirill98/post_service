package faang.school.postservice.dto.like;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LikeDto {
    private long id;

    private long userId;

    private Long postId;
    private Long commentId;
}
