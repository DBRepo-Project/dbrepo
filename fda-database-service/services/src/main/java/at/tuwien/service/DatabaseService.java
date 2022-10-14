package at.tuwien.service;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;

import java.security.Principal;
import java.util.List;

public interface DatabaseService {

    /**
     * Finds all databases in the metadata database for a given container id.
     *
     * @param containerId The container id.
     * @return A list of databases
     */
    List<Database> findAll(Long containerId);

    /**
     * Finds a specific database for a given id in the metadata database.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param principal   The principal.
     * @return The database if found.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database findPublicOrMineById(Long containerId, Long databaseId, Principal principal)
            throws DatabaseNotFoundException;

    /**
     * Find a database by id, only used in the authentication service
     *
     * @param containerId the container id.
     * @param databaseId  the database id.
     * @return The database.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database findById(Long containerId, Long databaseId) throws DatabaseNotFoundException;

    /**
     * Deletes a database with given id in the metadata database. Side effects: does only mark the database as deleted,
     * does not actually delete it.
     *
     * @param id         The container id.
     * @param databaseId The database id.
     * @throws DatabaseNotFoundException  The database was not found.
     * @throws ImageNotSupportedException The image is not supported.
     * @throws DatabaseMalformedException The query string is malformed.
     * @throws AmqpException              The exchange could not be deleted.
     */
    void delete(Long id, Long databaseId, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException,
            DatabaseMalformedException, AmqpException, ContainerConnectionException, ContainerNotFoundException, DatabaseConnectionException, QueryMalformedException;

    /**
     * Creates a new database with minimal metadata in the metadata database and creates a new database on the container.
     *
     * @param id        The container id.
     * @param createDto The metadata.
     * @return The created database as stored on the metadata database.
     * @throws ImageNotSupportedException   The image is not supported.
     * @throws ContainerNotFoundException   The container was not found.
     * @throws DatabaseMalformedException   The query string is malformed.
     * @throws AmqpException                The exchange could not be created.
     * @throws ContainerConnectionException The connection to the container did not establish.
     * @throws UserNotFoundException        The current user could not be loaded in the metadata database.
     */
    Database create(Long id, DatabaseCreateDto createDto, Principal principal)
            throws ImageNotSupportedException, ContainerNotFoundException,
            DatabaseMalformedException, AmqpException, ContainerConnectionException, UserNotFoundException, DatabaseNameExistsException, DatabaseConnectionException, QueryMalformedException;

    /**
     * Updates the visibility of the database.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param transferDto The visibility
     * @return The database.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database transfer(Long containerId, Long databaseId, DatabaseTransferDto transferDto)
            throws DatabaseNotFoundException;
}
