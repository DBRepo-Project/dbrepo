package at.tuwien.repository.sdb;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.entities.database.table.columns.TableColumnKey;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TableColumnIdxRepository extends ElasticsearchRepository<ColumnDto, TableColumnKey> {
}