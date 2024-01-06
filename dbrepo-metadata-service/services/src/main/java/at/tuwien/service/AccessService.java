package at.tuwien.service;

import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;

import java.util.List;
import java.util.UUID;

public interface AccessService {

    /**
     * Loads all database access definitions for a database with id.
     *
     * @param databaseId The database id.
     * @return The list of database access definitions.
     */
    List<DatabaseAccess> list(Long databaseId) throws DatabaseNotFoundException;

    /**
     * Finds database access by given database id and user id.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return The database access.
     * @throws AccessDeniedException The access does not exist.
     */
    DatabaseAccess find(Long databaseId, UUID userId) throws AccessDeniedException, DatabaseNotFoundException;

    /**
     * Give somebody access to a database of container.
     *
     * @param databaseId The database id.
     * @param accessDto  The access.
     * @param userId     The user id.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws UserNotFoundException      The authenticated user was not found in the metadata database.
     * @throws NotAllowedException        The access is not allowed.
     * @throws QueryMalformedException    The mapped access query is malformed.
     * @throws DatabaseMalformedException The database has an invalid state.
     */
    void create(Long databaseId, UUID userId, DatabaseGiveAccessDto accessDto) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseMalformedException;

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
    void update(Long databaseId, UUID userId, DatabaseModifyAccessDto accessDto) throws DatabaseNotFoundException,
            UserNotFoundException, QueryMalformedException, DatabaseMalformedException,
            NotAllowedException;

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
    void delete(Long databaseId, UUID userId) throws DatabaseNotFoundException, UserNotFoundException,
            NotAllowedException, QueryMalformedException, DatabaseMalformedException, AccessDeniedException;
}
