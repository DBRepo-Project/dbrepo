package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;

import java.util.List;

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
} 