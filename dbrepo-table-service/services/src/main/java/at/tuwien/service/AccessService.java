package at.tuwien.service;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.NotAllowedException;
import org.springframework.transaction.annotation.Transactional;

public interface AccessService {

    @Transactional(readOnly = true)
    DatabaseAccess find(Long databaseId, String username) throws NotAllowedException;

    /**
     * Checks if the user with username has access to the database with given id.
     *
     * @param databaseId The database id.
     * @param username   The username.
     * @return The access object.
     * @throws AccessDeniedException The user does not have access.
     */
    DatabaseAccess hasAccess(Long databaseId, String username) throws AccessDeniedException;
}
