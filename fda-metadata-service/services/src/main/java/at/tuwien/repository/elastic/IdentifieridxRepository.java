package at.tuwien.repository.elastic;

import at.tuwien.entities.identifier.Identifier;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentifieridxRepository extends ElasticsearchRepository<Identifier, Long> {
}