package at.tuwien.service;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.NotAllowedException;

public interface AccessService {

    /**
     * @param databaseId The database id.
     * @param username   The username.
     * @return
     * @throws NotAllowedException
     */
    DatabaseAccess find(Long databaseId, String username) throws NotAllowedException;
}
