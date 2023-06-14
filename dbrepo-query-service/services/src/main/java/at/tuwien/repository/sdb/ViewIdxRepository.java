package at.tuwien.repository.sdb;

import at.tuwien.entities.database.View;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViewIdxRepository extends ElasticsearchRepository<View, Long> {
}