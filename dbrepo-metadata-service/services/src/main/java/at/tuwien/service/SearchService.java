package at.tuwien.service;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.SearchServiceConnectionException;
import at.tuwien.exception.SearchServiceException;

public interface SearchService {

    DatabaseDto save(Database database) throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException;

    void delete(Long databaseId) throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException;
}
