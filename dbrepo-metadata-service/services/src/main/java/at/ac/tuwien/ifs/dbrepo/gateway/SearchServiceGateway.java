package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.util.UUID;

public interface SearchServiceGateway {

    DatabaseBriefDto update(Database database) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException;

    void delete(UUID databaseId) throws SearchServiceConnectionException, SearchServiceException, DatabaseNotFoundException;
}
