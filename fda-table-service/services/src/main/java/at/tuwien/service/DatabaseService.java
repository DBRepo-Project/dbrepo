package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;

import java.security.Principal;

public interface DatabaseService {

    /**
     * Finds a specific database for a given id in the metadata database.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param principal   The principal.
     * @return The database if found.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database findPublicOrMineById(Long containerId, Long databaseId, Principal principal) throws DatabaseNotFoundException;

    Database find(Long container, Long databaseId) throws DatabaseNotFoundException;
}
