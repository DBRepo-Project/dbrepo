package at.tuwien.service;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;

import java.util.List;

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
     * @return The database access, if successful.
     * @throws AccessNotFoundException The access was not found in the metadata database.
     */
    DatabaseAccess find(Database database, User user) throws AccessNotFoundException;

    /**
     * Give somebody access to a database of container.
     *
     * @param database The database.
     * @param access   The access.
     * @param user     The user.
     * @return The database access, if successful.
     * @throws DataServiceException           The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException The connection with the data service could not be established.
     * @throws DatabaseNotFoundException  The database was not found in the metadata/search database.
     */
    DatabaseAccess create(Database database, User user, AccessTypeDto access) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Update access to a database.
     *
     * @param database The database.
     * @param user     The user.
     * @param access   The updated access.
     * @throws DataServiceException           The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException The connection with the data service could not be established.
     * @throws DatabaseNotFoundException  The database was not found in the metadata/search database.
     */
    void update(Database database, User user, AccessTypeDto access) throws at.tuwien.exception.DataServiceException, DataServiceConnectionException,
            AccessNotFoundException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;

    /**
     * Revokes access to a database of container.
     *
     * @param database The database.
     * @param user     The user.
     * @throws DataServiceException           The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException The connection with the data service could not be established.
     * @throws DatabaseNotFoundException  The database was not found in the search database.
     */
    void delete(Database database, User user) throws AccessNotFoundException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;
}
