package at.tuwien.service;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;

import java.util.List;
import java.util.UUID;

public interface AccessService {

    List<DatabaseAccess> list(Long databaseId) throws NotAllowedException;

    /**
     * Finds database access by given database id and user id.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return The database access.
     * @throws AccessDeniedException The access does not exist.
     */
    DatabaseAccess find(Long databaseId, UUID userId) throws AccessDeniedException;

    /**
     * Checks if user with username has access to database with given id.
     *
     * @param databaseId The database id.
     * @param username   The username.
     * @return True if user has access, False otherwise.
     * @throws NotAllowedException The access is denied.
     */
    DatabaseAccess find(Long databaseId, String username) throws NotAllowedException;

    /**
     * Checks if the user with username has access to the database with given id.
     *
     * @param databaseId The database id.
     * @param username   The username.
     * @return The access object.
     * @throws NotAllowedException The user does not have access.
     */
    DatabaseAccess hasAccess(Long databaseId, String username) throws NotAllowedException;

    /**
     * Give somebody access to a database of container.
     *
     * @param databaseId The database id.
     * @param accessDto  The access.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws UserNotFoundException      The authenticated user was not found in the metadata database.
     * @throws NotAllowedException        The access is not allowed.
     * @throws QueryMalformedException    The mapped access query is malformed.
     * @throws DatabaseMalformedException The database has an invalid state.
     */
    void create(Long databaseId, DatabaseGiveAccessDto accessDto) throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException, KeycloakRemoteException, AccessDeniedException;

    /**
     * Update access to a database.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @param accessDto  The updated access.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws UserNotFoundException      The authenticated user was not found in the metadata database.
     * @throws NotAllowedException        The access is not allowed.
     * @throws QueryMalformedException    The mapped access query is malformed.
     * @throws DatabaseMalformedException The database has an invalid state.
     */
    void update(Long databaseId, UUID userId, DatabaseModifyAccessDto accessDto) throws DatabaseNotFoundException, UserNotFoundException, QueryMalformedException, DatabaseMalformedException,
            NotAllowedException, KeycloakRemoteException, AccessDeniedException;

    /**
     * Revokes access to a database of container.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws UserNotFoundException      The authenticated user was not found in the metadata database.
     * @throws NotAllowedException        The access is not allowed.
     * @throws QueryMalformedException    The mapped access query is malformed.
     * @throws DatabaseMalformedException The database has an invalid state.
     */
    void delete(Long databaseId, UUID userId) throws DatabaseNotFoundException, UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException, KeycloakRemoteException, AccessDeniedException;
}
