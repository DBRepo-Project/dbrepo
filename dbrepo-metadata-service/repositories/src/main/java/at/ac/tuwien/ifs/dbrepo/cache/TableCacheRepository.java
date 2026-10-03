package at.ac.tuwien.ifs.dbrepo.cache;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TableCacheRepository extends CrudRepository<Table, UUID> {
}
