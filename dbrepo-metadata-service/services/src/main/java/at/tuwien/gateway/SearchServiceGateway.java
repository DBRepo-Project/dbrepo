package at.tuwien.gateway;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;

public interface SearchServiceGateway {

    DatabaseDto save(Database database) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException;

    void delete(Long databaseId) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException;
}
