package at.tuwien.service;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
public interface DatabaseService {

    /**
     * Finds all databases stored in the metadata database.
     *
     * @return List of databases.
     */
    List<Database> findAll();

    /**
     * Finds a specific database for a given id in the metadata database.
     *
     * @param databaseId  The database id.
     * @return The database if found.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database find(Long databaseId) throws DatabaseNotFoundException;

    /**
     * Finds a specific database for a given id in the metadata database.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database findPublicOrMineById(Long databaseId, UUID userId) throws DatabaseNotFoundException;

    /**
     * Find a database by id, only used in the authentication service
     *
     * @param databaseId the database id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database findById(Long databaseId) throws DatabaseNotFoundException;

    /**
     * Deletes a database with given id in the metadata database. Side effects: does only mark the database as deleted,
     * does not actually delete it.
     *
     * @param databaseId The database id.
     * @param userId     The user id.
     * @throws DatabaseNotFoundException    The database was not found in the metadata database.
     * @throws ImageNotSupportedException   The image is not supported.
     * @throws DatabaseMalformedException   The query string is malformed.
     * @throws AmqpException                The exchange could not be deleted.
     * @throws DatabaseConnectionException  The connection to the database could not be established by the database connector.
     * @throws QueryMalformedException      The mapped deletion query resulted in an invalid query statement and thus was rejected by the database engine.
     * @throws UserNotFoundException        The current user could not be loaded in the metadata database.
     */
    void delete(Long databaseId, UUID userId)
            throws DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseMalformedException, AmqpException,
            DatabaseConnectionException, QueryMalformedException, UserNotFoundException;

    /**
     * Creates a new database with minimal metadata in the metadata database and creates a new database on the container.
     *
     * @param createDto The metadata.
     * @return The database, if successful.
     * @throws ImageNotSupportedException   The image is not supported.
     * @throws ContainerNotFoundException   The container was not found in the metadata database.
     * @throws DatabaseMalformedException   The query string is malformed.
     * @throws AmqpException                The exchange could not be created.
     * @throws ContainerConnectionException The connection to the container could not be established.
     * @throws UserNotFoundException        The current user could not be loaded in the metadata database.
     * @throws DatabaseNameExistsException  A database with this name already exists in the container.
     * @throws DatabaseConnectionException  The connection to the database could not be established by the database connector.
     * @throws QueryMalformedException      The mapped creation query resulted in an invalid query statement and thus was rejected by the database engine.
     */
    Database create(DatabaseCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException,
            DatabaseMalformedException, AmqpException, ContainerConnectionException, UserNotFoundException,
            DatabaseNameExistsException, DatabaseConnectionException, QueryMalformedException;

    /**
     * Updates the visibility of the database.
     *
     * @param databaseId The database id.
     * @param data       The visibility
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database visibility(Long databaseId, DatabaseModifyVisibilityDto data) throws DatabaseNotFoundException;

    /**
     * Transfer ownership of a database
     *
     * @param databaseId  The database id.
     * @param transferDto The payload with the new owner.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     * @throws UserNotFoundException     The new user was not found in the metadata database.
     */
    Database transfer(Long databaseId, DatabaseTransferDto transferDto) throws DatabaseNotFoundException, UserNotFoundException;
}
