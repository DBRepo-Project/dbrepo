package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaTableLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReplicationService {

    /**
     * Sends database replication notification to other instances.
     * @param databaseNotificationDto The database notification to replicate
     * @param replicaUrls List of URLs to send the replication notification to
     */
    void sendDatabaseReplicationToInstances(DatabaseNotificationDto databaseNotificationDto, List<String> replicaUrls);

    /**
     * Sends database replication notification to a specific instance.
     * @param databaseNotificationDto The database notification to replicate
     * @param replicaUrl The URL of the instance to send the notification to
     * @return The database ID returned from the replication, or null if not available
     */
    String sendDatabaseReplicationToInstance(DatabaseNotificationDto databaseNotificationDto, String replicaUrl);

    /**
     * Sends table replication notification to other instances.
     * @param tableNotificationDto The table notification to replicate
     */
    void sendTableReplicationToInstances(TableNotificationDto tableNotificationDto);

    /**
     * Sends table replication notification to a specific instance.
     * @param remoteDatabaseId The remote database ID
     * @param tableNotificationDto The table notification to replicate
     * @param replicaUrl The URL of the instance to send the notification to
     * @return The table ID returned from the replication, or null if not available
     */
    String sendTableReplicationToInstance(UUID remoteDatabaseId, TableNotificationDto tableNotificationDto, String replicaUrl);
    
    /**
     * Updates the replication URL with the remote database ID.
     * @param databaseId The local database ID
     * @param replicaUrl The replica URL to update
     * @param remoteDatabaseId The remote database ID
     */
    void updateReplicationUrlWithRemoteId(UUID databaseId, String replicaUrl, UUID remoteDatabaseId);

    void updateTableReplicationUrlWithRemoteId(UUID databaseId, UUID localTableId, String replicaUrl, UUID remoteTableId);
} 