package faang.school.postservice.dto.post;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {
  private long id;
  private String content;
  private long authorId;
  private long projectId;
  private int likeCount;
}
