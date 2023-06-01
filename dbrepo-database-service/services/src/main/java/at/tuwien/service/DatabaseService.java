package at.tuwien.service;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
public interface DatabaseService {

    /**
     * Finds all databases in the metadata database for a given container id.
     *
     * @param containerId The container id.
     * @return List of databases.
     */
    List<Database> findAll(Long containerId);

    /**
     * Finds a specific database for a given id in the metadata database.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param principal   The principal.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database findPublicOrMineById(Long containerId, Long databaseId, Principal principal)
            throws DatabaseNotFoundException;

    /**
     * Find a database by id, only used in the authentication service
     *
     * @param containerId the container id.
     * @param databaseId  the database id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database findById(Long containerId, Long databaseId) throws DatabaseNotFoundException;

    /**
     * Deletes a database with given id in the metadata database. Side effects: does only mark the database as deleted,
     * does not actually delete it.
     *
     * @param id         The container id.
     * @param databaseId The database id.
     * @throws DatabaseNotFoundException    The database was not found in the metadata database.
     * @throws ImageNotSupportedException   The image is not supported.
     * @throws DatabaseMalformedException   The query string is malformed.
     * @throws AmqpException                The exchange could not be deleted.
     * @throws ContainerConnectionException The connection to the container could not be established.
     * @throws ContainerNotFoundException   The container was not found in the metadata database.
     * @throws DatabaseConnectionException  The connection to the database could not be established by the database connector.
     * @throws QueryMalformedException      The mapped deletion query resulted in an invalid query statement and thus was rejected by the database engine.
     * @throws UserNotFoundException        The current user could not be loaded in the metadata database.
     */
    void delete(Long id, Long databaseId, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseMalformedException, AmqpException, ContainerConnectionException, ContainerNotFoundException,
            DatabaseConnectionException, QueryMalformedException, UserNotFoundException;

    /**
     * Creates a new database with minimal metadata in the metadata database and creates a new database on the container.
     *
     * @param id        The container id.
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
    Database create(Long id, DatabaseCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException,
            DatabaseMalformedException, AmqpException, ContainerConnectionException, UserNotFoundException,
            DatabaseNameExistsException, DatabaseConnectionException, QueryMalformedException;

    /**
     * Updates the visibility of the database.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param data        The visibility
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database visibility(Long containerId, Long databaseId, DatabaseModifyVisibilityDto data)
            throws DatabaseNotFoundException;

    /**
     * Transfer ownership of a database
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param transferDto The payload with the new owner.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     * @throws UserNotFoundException     The new user was not found in the metadata database.
     */
    Database transfer(Long containerId, Long databaseId, DatabaseTransferDto transferDto)
            throws DatabaseNotFoundException, UserNotFoundException;
}
