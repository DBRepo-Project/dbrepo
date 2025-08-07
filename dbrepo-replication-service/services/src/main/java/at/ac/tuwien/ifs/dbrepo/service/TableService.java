package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TableService {

    void handleTableReplication(List<ReplicaLocation> replicas, CreateTableDto createTableDto);

    Map<String, Object> insertReplicatedTable(UUID databaseId, CreateTableDto createTableDto);
}
