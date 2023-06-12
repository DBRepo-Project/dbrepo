package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;

public interface DatabaseService {

    /**
     * Finds a specific database for a given id in the metadata database.
     *
     * @param databaseId  The database id.
     * @return The database if found.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database find(Long databaseId) throws DatabaseNotFoundException;
}
