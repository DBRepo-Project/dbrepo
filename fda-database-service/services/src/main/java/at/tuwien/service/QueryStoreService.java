package at.tuwien.service;

import at.tuwien.exception.DatabaseConnectionException;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.DatabaseNotFoundException;

public interface QueryStoreService {

    void create(Long containerId, Long databaseId) throws DatabaseNotFoundException, DatabaseConnectionException, DatabaseMalformedException;
}
