package at.tuwien.repository.elastic;

import at.tuwien.entities.database.Database;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseIdxRepository extends ElasticsearchRepository<Database, Long> {
}