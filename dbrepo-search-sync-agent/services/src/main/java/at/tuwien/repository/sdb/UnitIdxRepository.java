package at.tuwien.repository.sdb;

import at.tuwien.api.database.table.columns.concepts.UnitDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitIdxRepository extends ElasticsearchRepository<UnitDto, String> {
}