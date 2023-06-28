package at.tuwien.repository.sdb;

import at.tuwien.entities.identifier.Identifier;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentifierIdxRepository extends ElasticsearchRepository<Identifier, Long> {
}