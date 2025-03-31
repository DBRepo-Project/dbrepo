package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.security.Principal;
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
     * Finds database access by given database and user, where the access is determined by the username (needed since {@link Principal#getName()} embeds the username).
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
     * @throws DataServiceException             The data service responded with an unexpected error code.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws DatabaseNotFoundException        The database was not found in the metadata/search database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    DatabaseAccess create(Database database, User user, AccessTypeDto access) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    /**
     * Update access to a database.
     *
     * @param database The database.
     * @param user     The user.
     * @param access   The updated access.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws AccessNotFoundException          The access was not found.
     * @throws DatabaseNotFoundException        The database was not found in the metadata/search database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    void update(Database database, User user, AccessTypeDto access) throws DataServiceException,
            DataServiceConnectionException, AccessNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    /**
     * Revokes access to a database of container.
     *
     * @param database The database.
     * @param user     The user.
     * @throws AccessNotFoundException          The access was not found.
     * @throws DataServiceException             The data service responded with unexpected behavior.
     * @throws DataServiceConnectionException   The connection with the data service could not be established.
     * @throws DatabaseNotFoundException        The database was not found in the metadata/search database.
     * @throws SearchServiceException           The search service responded with an unexpected error code.
     * @throws SearchServiceConnectionException The connection with the search service could not be established.
     */
    void delete(Database database, User user) throws AccessNotFoundException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException;
}
