package at.tuwien.repository.elastic;

import at.tuwien.api.database.table.columns.ColumnDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableColumnIdxRepository extends ElasticsearchRepository<ColumnDto, Long> {
}