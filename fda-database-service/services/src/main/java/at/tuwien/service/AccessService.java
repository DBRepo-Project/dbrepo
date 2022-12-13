package at.tuwien.service;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AccessService {

    List<DatabaseAccess> list(Long databaseId) throws AccessDeniedException;

    /**
     * Checks if user with username has access to database with given id.
     *
     * @param databaseId The database id.
     * @param username   The username.
     * @return True if user has access, false otherwise.
     */
    DatabaseAccess find(Long databaseId, String username) throws AccessDeniedException;

    /**
     * Give somebody access to a database of container.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param accessDto   The access.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     * @throws UserNotFoundException     The authenticated user was not found in the metadata database.
     */
    void create(Long containerId, Long databaseId, DatabaseGiveAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException;

    void update(Long containerId, Long databaseId, String username, DatabaseModifyAccessDto accessDto)
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
    void delete(Long containerId, Long databaseId, String username)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException;
}
