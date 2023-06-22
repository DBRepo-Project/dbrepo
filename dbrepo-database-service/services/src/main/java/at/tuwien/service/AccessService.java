package at.tuwien.service;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;

import java.util.List;

public interface AccessService {

    List<DatabaseAccess> list(Long databaseId) throws AccessDeniedException;

    /**
     * Checks if user with username has access to database with given id.
     *
     * @param databaseId The database id.
     * @param username   The username.
     * @return True if user has access, False otherwise.
     * @throws AccessDeniedException The access is denied.
     */
    DatabaseAccess find(Long databaseId, String username) throws AccessDeniedException;

    /**
     * Give somebody access to a database of container.
     *
     * @param databaseId  The database id.
     * @param accessDto   The access.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws UserNotFoundException      The authenticated user was not found in the metadata database.
     * @throws NotAllowedException        The access is not allowed.
     * @throws QueryMalformedException    The mapped access query is malformed.
     * @throws DatabaseMalformedException The database has an invalid state.
     */
    void create(Long databaseId, DatabaseGiveAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException;

    /**
     * Update access to a database.
     *
     * @param databaseId  The database id.
     * @param username    The username.
     * @param accessDto   The updated access.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws UserNotFoundException      The authenticated user was not found in the metadata database.
     * @throws NotAllowedException        The access is not allowed.
     * @throws QueryMalformedException    The mapped access query is malformed.
     * @throws DatabaseMalformedException The database has an invalid state.
     */
    void update(Long databaseId, String username, DatabaseModifyAccessDto accessDto)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException, AccessDeniedException;

    /**
     * Revokes access to a database of container.
     *
     * @param databaseId  The database id.
     * @param username    The user name.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws UserNotFoundException      The authenticated user was not found in the metadata database.
     * @throws NotAllowedException        The access is not allowed.
     * @throws QueryMalformedException    The mapped access query is malformed.
     * @throws DatabaseMalformedException The database has an invalid state.
     */
    void delete(Long databaseId, String username)
            throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException;
}
