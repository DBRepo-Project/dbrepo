package at.ac.tuwien.ifs.dbrepo.cache;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCacheRepository extends CrudRepository<User, String> {
}
