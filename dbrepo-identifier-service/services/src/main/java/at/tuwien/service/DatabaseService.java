package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;

public interface DatabaseService {

    /**
     * Finds a database by given id in the metadata database.
     *
     * @param databaseId The database id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database with this id was not found in the metadata database.
     */
    Database find(Long databaseId) throws DatabaseNotFoundException;
}
