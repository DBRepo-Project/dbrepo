package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TableService {

    /**
     * Handles table replication by creating the table locally and sending notifications to other instances.
     * @param databaseId The database ID where the table should be created
     * @param replicas List of replica locations to send notifications to
     * @param createTableDto The table creation data to replicate
     * @param creationId The local table ID (creation ID) for updating replication URLs
     * @return The created table ID
     */
    String handleTableReplication(UUID databaseId, List<ReplicaLocation> replicas, CreateTableDto createTableDto, UUID creationId);

    Map<String, Object> insertReplicatedTable(UUID databaseId, CreateTableDto createTableDto);
}
