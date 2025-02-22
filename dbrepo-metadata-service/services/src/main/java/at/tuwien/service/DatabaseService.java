package at.tuwien.service;

import at.tuwien.api.database.CreateDatabaseDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import org.springframework.stereotype.Service;

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

    List<Database> findAllPublicOrSchemaPublic();

    List<Database> findAllPublicOrSchemaPublicOrReadAccessByInternalName(UUID userId, String internalName);

    /**
     * Finds all databases stored in the metadata database.
     *
     * @param userId The user id.
     * @return List of databases.
     */
    List<Database> findAllAtLestReadAccess(UUID userId);

    /**
     * Finds all databases stored in the metadata database.
     *
     * @param userId The user id.
     * @return List of databases.
     */
    List<Database> findAllPublicOrSchemaPublicOrReadAccess(UUID userId);

    /**
     * @param internalName The database internal name.
     * @return The databases if found.
     */
    List<Database> findAllPublicOrSchemaPublicByInternalName(String internalName);

    /**
     * Find a database by id, only used in the authentication service
     *
     * @param databaseId the database id.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database findById(UUID databaseId) throws DatabaseNotFoundException;

    /**
     * Creates a new database with minimal metadata in the metadata database and creates a new database on the container.
     *
     * @param container The container.
     * @param createDto The metadata.
     * @param user      The user.
     * @param internalUsers      The list of internal users.
     * @return The database, if successful.
     * @throws UserNotFoundException          If the container/user was not found in the metadata database.
     * @throws DataServiceException           If the data service returned non-successfully.
     * @throws DataServiceConnectionException If failing to connect to the data service/search service.
     */
    Database create(Container container, CreateDatabaseDto createDto, User user, List<User> internalUsers) throws UserNotFoundException,
            ContainerNotFoundException, DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException;

    /**
     * Updates the user's password.
     *
     * @param database The database.
     * @param user     The user.
     * @throws DataServiceException           If the data service returned non-successfully.
     * @throws DataServiceConnectionException If failing to connect to the data service.
     */
    void updatePassword(Database database, User user) throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException;

    /**
     * Updates the visibility of the database.
     *
     * @param database The database.
     * @param data     The visibility
     * @return The database, if successful.
     * @throws NotFoundException              The database was not found in the metadata database.
     * @throws DataServiceConnectionException If failing to connect to the search service.
     */
    Database modifyVisibility(Database database, DatabaseModifyVisibilityDto data) throws DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Transfer ownership of a database
     *
     * @param database The database.
     * @param user     The payload with the new owner.
     * @return The database, if successful.
     * @throws DatabaseNotFoundException The database was not found in the metadata database.
     */
    Database modifyOwner(Database database, User user) throws DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Modify image of database with given id.
     *
     * @param database The database.
     * @param image    The image.
     * @return The database, if successful.
     */
    Database modifyImage(Database database, byte[] image) throws DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;

    Database updateTableMetadata(Database database) throws DatabaseNotFoundException, DataServiceException,
            SearchServiceException, SearchServiceConnectionException, QueryNotFoundException,
            DataServiceConnectionException, MalformedException, TableNotFoundException;

    Database updateViewMetadata(Database database) throws DatabaseNotFoundException, DataServiceException,
            SearchServiceException, SearchServiceConnectionException, QueryNotFoundException,
            DataServiceConnectionException, ViewNotFoundException;
}
