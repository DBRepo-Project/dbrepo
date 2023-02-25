package at.tuwien.service;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.NotAllowedException;

public interface AccessService {

    /**
     * Find the access granted to a database with given id to a user with given username.
     *
     * @param databaseId The database id.
     * @param username   The username.
     * @return The access, if successful.
     * @throws NotAllowedException The access operation is not permitted.
     */
    DatabaseAccess find(Long databaseId, String username) throws NotAllowedException;
}
