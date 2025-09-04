package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TableService {


    String handleTableReplication(TableNotificationDto tableNotificationDto);

    Map<String, Object> insertReplicatedTable(UUID databaseId, TableNotificationDto tableNotificationDto);

    void handleDataReplication(DataReplicationDto dataReplicationDto);

    void handleDataDeleteReplication(DataReplicationDto dataReplicationDto);
}
