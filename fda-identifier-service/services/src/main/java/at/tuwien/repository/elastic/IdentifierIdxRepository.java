package at.tuwien.repository.elastic;

import at.tuwien.entities.identifier.Identifier;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository(value = "ElasticIdentifierService")
public interface IdentifierIdxRepository extends ElasticsearchRepository<Identifier, Long> {
}