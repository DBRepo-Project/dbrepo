package at.tuwien.service;

import at.tuwien.exception.*;

import java.security.Principal;

public interface QueryStoreService {

    void create(Long containerId, Long databaseId, Principal principal) throws DatabaseNotFoundException, DatabaseConnectionException, DatabaseMalformedException, UserNotFoundException, QueryStoreException;
}
