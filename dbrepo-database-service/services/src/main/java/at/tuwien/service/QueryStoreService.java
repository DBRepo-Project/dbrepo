package at.tuwien.service;

import at.tuwien.exception.*;

import java.security.Principal;

public interface QueryStoreService {

    /**
     * Creates a query in the Query Store (inside the user database).
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param principal   The user principal.
     * @throws DatabaseNotFoundException   The database was not found in the metadata database.
     * @throws DatabaseConnectionException The connection to the database could not be established by the database connector.
     * @throws DatabaseMalformedException  The query string is malformed.
     * @throws UserNotFoundException       The current user could not be loaded in the metadata database.
     * @throws QueryStoreException         The Query Store rejected the creation of a query.
     */
    void create(Long containerId, Long databaseId, Principal principal) throws DatabaseNotFoundException,
            DatabaseConnectionException, DatabaseMalformedException, UserNotFoundException, QueryStoreException;
}
