package at.tuwien.repository.sdb;

import at.tuwien.api.database.ViewDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViewIdxRepository extends ElasticsearchRepository<ViewDto, Long> {
}