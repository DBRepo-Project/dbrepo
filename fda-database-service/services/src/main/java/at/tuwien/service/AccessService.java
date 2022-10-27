package at.tuwien.service;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;

public interface AccessService {

    /**
     * Checks if user with username has access to database with given id.
     *
     * @param databaseId The database id.
     * @param username   The username.
     * @return True if user has access, false otherwise.
     */
    DatabaseAccess hasAccess(Long databaseId, String username) throws AccessDeniedException;

    /**
     * Give somebody access to a database of container.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param accessDto   The access.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     * @throws UserNotFoundException     The authenticated user was not found in the metadata database.
     */
    void giveAccess(Long containerId, Long databaseId, DatabaseGiveAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException;

    void modifyAccess(Long containerId, Long databaseId, String username, DatabaseModifyAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException;

    /**
     * Revokes access to a database of container.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param username    The user name.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     * @throws UserNotFoundException     The authenticated user was not found in the metadata database.
     */
    void revokeAccess(Long containerId, Long databaseId, String username)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException;
}
