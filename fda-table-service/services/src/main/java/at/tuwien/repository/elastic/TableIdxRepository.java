package at.tuwien.repository.elastic;

import at.tuwien.api.database.table.TableDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableIdxRepository extends ElasticsearchRepository<TableDto, Long> {
}