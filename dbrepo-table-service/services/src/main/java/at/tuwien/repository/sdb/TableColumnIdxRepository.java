package at.tuwien.repository.sdb;

import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnKey;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TableColumnIdxRepository extends ElasticsearchRepository<TableColumn, TableColumnKey> {
}