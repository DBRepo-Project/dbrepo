package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;

public interface DatabaseService {

    /**
     * Finds a database by given id in the remote database service.
     *
     * @param databaseId  The database id.
     * @return The database.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database find(Long databaseId) throws DatabaseNotFoundException;
}
