package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DatabaseService {

    /**
     * Finds all databases stored in the metadata database.
     *
     * @return List of databases.
     */
    List<Database> findAll();

    /**
     * Finds a specific database for a given id in the metadata database.
     *
     * @param databaseId The database id.
     * @return The database if found.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database find(Long databaseId) throws DatabaseNotFoundException;

    /**
     * Finds a specific database for a given internal name in the metadata database.
     *
     * @param internalName The database internal name.
     * @return The database if found.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database findByInternalName(String internalName) throws DatabaseNotFoundException;

}
