package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;

import java.util.List;

public interface DatabaseService {

    /**
     * Finds a database by given id in the metadata database.
     *
     * @param databaseId  The database id.
     * @return The database.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database find(Long databaseId) throws DatabaseNotFoundException;

    /**
     * Finds all databases stored in the metadata database.
     *
     * @return List of databases.
     */
    List<Database> findAll();
}
