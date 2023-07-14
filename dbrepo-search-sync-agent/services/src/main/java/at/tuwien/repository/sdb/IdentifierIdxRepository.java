package at.tuwien.repository.sdb;

import at.tuwien.api.identifier.IdentifierDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentifierIdxRepository extends ElasticsearchRepository<IdentifierDto, String> {
}