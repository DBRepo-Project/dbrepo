package at.tuwien.repository.sdb;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptIdxRepository extends ElasticsearchRepository<ConceptDto, String> {
}