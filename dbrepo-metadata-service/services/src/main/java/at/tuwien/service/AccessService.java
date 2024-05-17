package at.tuwien.service;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface AccessService {

    /**
     * Loads all database access definitions for a database with id.
     *
     * @param database The database.
     * @return The list of database access definitions.
     */
    List<DatabaseAccess> list(Database database);

    /**
     * Finds database access by given database and user.
     *
     * @param database The database.
     * @param user     The user.
     * @return The database access.
     * @throws AccessNotFoundException The access was not found in the metadata database.
     */
    DatabaseAccess find(Database database, User user) throws AccessNotFoundException;

    /**
     * Give somebody access to a database of container.
     *
     * @param database The database.
     * @param access   The access.
     * @param user     The user.
     * @throws ServiceException           The data service responded with unexpected behavior.
     * @throws ServiceConnectionException The connection with the data service could not be established.
     * @throws DatabaseNotFoundException  The database was not found in the metadata/search database.
     */
    void create(Database database, User user, AccessTypeDto access) throws ServiceException, ServiceConnectionException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Update access to a database.
     *
     * @param database The database.
     * @param user     The user.
     * @param access   The updated access.
     * @throws ServiceException           The data service responded with unexpected behavior.
     * @throws ServiceConnectionException The connection with the data service could not be established.
     * @throws DatabaseNotFoundException  The database was not found in the metadata/search database.
     */
    void update(Database database, User user, AccessTypeDto access) throws ServiceException, ServiceConnectionException,
            AccessNotFoundException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Revokes access to a database of container.
     *
     * @param database The database.
     * @param user     The user.
     * @throws ServiceException           The data service responded with unexpected behavior.
     * @throws ServiceConnectionException The connection with the data service could not be established.
     * @throws DatabaseNotFoundException  The database was not found in the search database.
     */
    void delete(Database database, User user) throws AccessNotFoundException, ServiceException,
            ServiceConnectionException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;
}
