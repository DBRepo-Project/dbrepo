package at.tuwien.repository.elastic;

import at.tuwien.api.database.DatabaseDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseIdxRepository extends ElasticsearchRepository<DatabaseDto, Long> {
}
