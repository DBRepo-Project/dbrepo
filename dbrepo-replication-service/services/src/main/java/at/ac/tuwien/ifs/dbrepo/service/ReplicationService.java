package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;

import java.util.List;
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
     */
    void sendDatabaseReplicationToInstance(DatabaseNotificationDto databaseNotificationDto, String replicaUrl);

    /**
     * Sends table replication notification to other instances.
     * @param databaseId The database ID where the table should be created
     * @param createTableDto The table creation data to replicate
     * @param replicaUrls List of URLs to send the replication notification to
     */
    void sendTableReplicationToInstances(UUID databaseId, CreateTableDto createTableDto, List<String> replicaUrls);

    /**
     * Sends table replication notification to a specific instance.
     * @param databaseId The database ID where the table should be created
     * @param createTableDto The table creation data to replicate
     * @param replicaUrl The URL of the instance to send the notification to
     */
    void sendTableReplicationToInstance(UUID databaseId, CreateTableDto createTableDto, String replicaUrl);
} 