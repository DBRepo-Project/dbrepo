package at.tuwien.repository.elastic;

import at.tuwien.entities.database.table.Table;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableidxRepository extends ElasticsearchRepository<Table, Long> {
}