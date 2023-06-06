package at.tuwien.repository.sdb;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnKeyDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableColumnIdxRepository extends ElasticsearchRepository<ColumnDto, ColumnKeyDto> {
}