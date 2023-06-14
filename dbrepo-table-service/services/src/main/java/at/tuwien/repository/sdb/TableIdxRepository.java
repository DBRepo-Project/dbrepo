package at.tuwien.repository.sdb;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.TableKey;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableIdxRepository extends ElasticsearchRepository<Table, TableKey> {
}