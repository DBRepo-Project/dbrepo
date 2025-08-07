package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;

import java.util.Map;

public interface DatabaseService {


    /**
     * Handles database replication notification.
     * @param databaseNotificationDto The database notification containing replication information.
     */
    void handleDatabaseReplication(DatabaseNotificationDto databaseNotificationDto);

    /**
     * Creates a database locally by calling the metadata service
     *
     * @param databaseNotificationDto The database notification containing replication information
     * @return The response from the metadata service with database ID
     */
    Map<String, Object> insertReplicatedDatabase(DatabaseNotificationDto databaseNotificationDto);
}
