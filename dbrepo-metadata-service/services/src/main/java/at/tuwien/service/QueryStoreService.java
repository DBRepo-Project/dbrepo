package at.tuwien.service;

import at.tuwien.exception.*;

import java.security.Principal;

public interface QueryStoreService {

    /**
     * Creates the query store in the database.
     *
     * @param databaseId  The database id.
     * @param principal   The principal of the user.
     * @throws DatabaseNotFoundException
     * @throws DatabaseConnectionException
     * @throws DatabaseMalformedException
     * @throws UserNotFoundException
     * @throws QueryStoreException
     */
    void create(Long databaseId, Principal principal) throws DatabaseNotFoundException, DatabaseConnectionException, DatabaseMalformedException, UserNotFoundException, QueryStoreException;
}
