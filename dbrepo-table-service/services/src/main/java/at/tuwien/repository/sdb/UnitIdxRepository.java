package at.tuwien.repository.sdb;

import at.tuwien.entities.database.table.columns.TableColumnUnit;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitIdxRepository extends ElasticsearchRepository<TableColumnUnit, String> {
}