package at.tuwien.repository.sdb;

import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.TableKeyDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableIdxRepository extends ElasticsearchRepository<TableDto, TableKeyDto> {
}