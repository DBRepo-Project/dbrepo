package at.tuwien.service;

import at.tuwien.exception.*;

import java.security.Principal;

public interface QueryStoreService {

    /**
     * Creates the query store in the database.
     *
     * @param databaseId The database id.
     * @param principal  The principal of the user.
     * @throws DatabaseNotFoundException  The database is not found in the metadata database.
     * @throws DatabaseMalformedException The database is malformed.
     * @throws UserNotFoundException      The user was not found in the metadata database.
     * @throws QueryStoreException        The query store failed to retrieve.
     */
    void create(Long databaseId, Principal principal) throws DatabaseNotFoundException, DatabaseMalformedException,
            UserNotFoundException, QueryStoreException;
}
