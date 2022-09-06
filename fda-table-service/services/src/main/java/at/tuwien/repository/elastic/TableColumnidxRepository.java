package at.tuwien.repository.elastic;

import at.tuwien.entities.database.table.columns.TableColumn;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableColumnidxRepository extends ElasticsearchRepository<TableColumn, Long> {
}