package faang.school.postservice.mapper;

import faang.school.postservice.dto.UserDto;
import faang.school.postservice.model.redis.RedisUser;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserDto toDto(RedisUser redisUser);
}
