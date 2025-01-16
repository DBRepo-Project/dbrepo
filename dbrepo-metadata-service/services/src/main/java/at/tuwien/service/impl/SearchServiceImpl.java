package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.SearchServiceConnectionException;
import at.tuwien.exception.SearchServiceException;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.service.SearchService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class SearchServiceImpl implements SearchService {

    private final SearchServiceGateway searchServiceGateway;

    @Autowired
    public SearchServiceImpl(SearchServiceGateway searchServiceGateway) {
        this.searchServiceGateway = searchServiceGateway;
    }

    @Override
    public DatabaseDto save(Database database) throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException {
        if (!database.getIsPublic() || !database.getIsSchemaPublic()) {
            log.warn("Database with id {} cannot be saved to be visible in search", database.getId());
            return null;
        }
        database.setTables(database.getTables()
                .stream()
                .filter(t -> t.getIsPublic() || t.getIsSchemaPublic())
                .toList());
        database.setViews(database.getViews()
                .stream()
                .filter(v -> v.getIsPublic() || v.getIsSchemaPublic())
                .toList());
        return searchServiceGateway.save(database);
    }

    @Override
    public void delete(Long databaseId) throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException {
        searchServiceGateway.delete(databaseId);
    }

}
