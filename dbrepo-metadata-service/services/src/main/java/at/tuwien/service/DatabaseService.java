package at.tuwien.service;

import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
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
     * Finds all databases stored in the metadata database.
     *
     * @param userId The user id.
     * @return List of databases.
     */
    List<Database> findAllAccess(UUID userId);

    /**
     * @param internalName The database internal name.
     * @return The database if found.
     * @throws DatabaseNotFoundException The database was not found.
     */
    Database findByInternalName(String internalName) throws DatabaseNotFoundException;

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
     * @param user      The user.
     * @return The database, if successful.
     * @throws UserNotFoundException      If the container/user was not found in the metadata database.
     * @throws ServiceException           If the data service returned non-successfully.
     * @throws ServiceConnectionException If failing to connect to the data service/search service.
     */
    Database create(DatabaseCreateDto createDto, User user) throws UserNotFoundException, ContainerNotFoundException,
            ServiceException, ServiceConnectionException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Updates the user's password.
     *
     * @param database The database.
     * @param user     The user.
     * @throws ServiceException           If the data service returned non-successfully.
     * @throws ServiceConnectionException If failing to connect to the data service.
     */
    void updatePassword(Database database, User user) throws ServiceException, ServiceConnectionException, DatabaseNotFoundException;

    /**
     * Updates the visibility of the database.
     *
     * @param database The database.
     * @param data     The visibility
     * @return The database, if successful.
     * @throws NotFoundException          The database was not found in the metadata database.
     * @throws ServiceConnectionException If failing to connect to the search service.
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


}
