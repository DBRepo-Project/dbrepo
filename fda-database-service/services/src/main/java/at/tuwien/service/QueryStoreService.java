package at.tuwien.service;

import at.tuwien.exception.DatabaseConnectionException;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.UserNotFoundException;

import java.security.Principal;

public interface QueryStoreService {

    void create(Long containerId, Long databaseId, Principal principal) throws DatabaseNotFoundException, DatabaseConnectionException, DatabaseMalformedException, UserNotFoundException;
}
