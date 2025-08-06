package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;

public interface DatabaseService {


    /**
     * Handles database replication notification.
     * @param databaseNotificationDto The database notification containing replication information.
     */
    void handleDatabaseReplication(DatabaseNotificationDto databaseNotificationDto);
}
