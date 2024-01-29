package at.tuwien.service;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyImageDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Finds all databases where the user with given id has access to.
     *
     * @param userId The user id.
     * @return The list of databases.
     */
    List<Database> findAccess(UUID userId);

    /**
     * Finds a specific database for a given id in the metadata database.
     *
     * @param databaseId The database id.
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
     * Creates a new database with minimal metadata in the metadata database and creates a new database on the container.
     *
     * @param createDto The metadata.
     * @return The database, if successful.
     * @throws ContainerNotFoundException The container was not found in the metadata database.
     * @throws DatabaseMalformedException The query string is malformed.
     * @throws UserNotFoundException      The current user could not be loaded in the metadata database.
     * @throws QueryMalformedException    The mapped creation query resulted in an invalid query statement and thus was rejected by the database engine.
     */
    Database create(DatabaseCreateDto createDto, Principal principal) throws ContainerNotFoundException,
            DatabaseMalformedException, UserNotFoundException, QueryMalformedException;

    /**
     * Updates the user's password.
     *
     * @param user The user.
     * @throws QueryMalformedException The mapped query is malformed.
     */
    void updatePassword(User user) throws QueryMalformedException;

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
    Database transfer(Long databaseId, DatabaseTransferDto transferDto) throws DatabaseNotFoundException,
            UserNotFoundException;

    /**
     * Modify image of database with given id.
     *
     * @param databaseId The database id.
     * @param image      The image.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database modifyImage(Long databaseId, byte[] image) throws DatabaseNotFoundException;

    /**
     * Obtain metadata from database with given id to read table and view information (schema) and write it to the metadata database for management by DBRepo.
     *
     * @param databaseId The database id.
     * @return The updated database.
     * @throws DatabaseNotFoundException  The database was not found in the metadata database.
     * @throws QueryMalformedException    The inspect query (table/view) is malformed and has syntax issues.
     * @throws DatabaseUnchangedException The metadata database is up-to-date and knows about all tables/views in the data database(s).
     * @throws ColumnParseException       The columns could not be automatically parsed from the views.
     */
    Database obtainMetadata(Long databaseId) throws DatabaseNotFoundException, QueryMalformedException,
            DatabaseUnchangedException, ColumnParseException, TableNotFoundException;
}
