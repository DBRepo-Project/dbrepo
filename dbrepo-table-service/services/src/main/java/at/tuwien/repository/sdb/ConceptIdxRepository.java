package at.tuwien.repository.sdb;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConceptIdxRepository extends ElasticsearchRepository<TableColumnConcept, String> {
}