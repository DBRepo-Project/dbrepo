package at.tuwien.service;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;

public interface AccessService {

    /**
     * Checks if the user with username has access to the table with given id in database with given id.
     *
     * @param databaseId  The database id.
     * @param tableId     The table id.
     * @param username    The username.
     * @return The access object.
     * @throws AccessDeniedException The user does not have access.
     */
    DatabaseAccess hasAccess(Long databaseId, Long tableId, String username) throws AccessDeniedException;
}
