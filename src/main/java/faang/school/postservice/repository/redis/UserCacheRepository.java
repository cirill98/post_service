package faang.school.postservice.repository.redis;

import faang.school.postservice.model.redis.UserForCache;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCacheRepository extends CrudRepository<UserForCache, Long> {
}
