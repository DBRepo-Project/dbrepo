package at.tuwien.gateway;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;

import java.util.UUID;

public interface SearchServiceGateway {

    DatabaseBriefDto update(Database database) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException;

    void delete(UUID databaseId) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException;
}
