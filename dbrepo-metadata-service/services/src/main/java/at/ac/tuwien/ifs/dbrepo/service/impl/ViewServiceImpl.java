package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ViewServiceImpl implements ViewService {

    private final MetadataMapper metadataMapper;
    private final DataServiceGateway dataServiceGateway;
    private final DatabaseRepository databaseRepository;
    private final SearchServiceGateway searchServiceGateway;
    private final DatabaseCacheRepository databaseCacheRepository;

    @Autowired
    public ViewServiceImpl(MetadataMapper metadataMapper, DataServiceGateway dataServiceGateway,
                           DatabaseRepository databaseRepository, SearchServiceGateway searchServiceGateway,
                           DatabaseCacheRepository databaseCacheRepository) {
        this.metadataMapper = metadataMapper;
        this.dataServiceGateway = dataServiceGateway;
        this.databaseRepository = databaseRepository;
        this.searchServiceGateway = searchServiceGateway;
        this.databaseCacheRepository = databaseCacheRepository;
    }

    @Override
    public View findById(Database database, UUID viewId) throws ViewNotFoundException {
        final Optional<View> optional = database.getViews()
                .stream()
                .filter(v -> v.getId().equals(viewId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find view with id: {}", viewId);
            throw new ViewNotFoundException("Failed to find view with id: " + viewId);
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void delete(View view) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, ViewNotFoundException, SearchServiceException, SearchServiceConnectionException {
        /* delete in data service */
        dataServiceGateway.deleteView(view.getDatabase().getId(), view.getId());
        /* delete in metadata database */
        view.getDatabase()
                .getViews()
                .remove(view);
        final Database database = databaseRepository.save(view.getDatabase());
        /* update cache */
        databaseCacheRepository.deleteById(view.getDatabase().getId());
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Deleted view with id {}", view.getId());
    }

    @Transactional
    @Override
    public void refresh(View view) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, ViewNotFoundException, SearchServiceException, SearchServiceConnectionException {
        /* refresh in data service */
        dataServiceGateway.refreshView(view.getDatabase().getId(), view.getId());
        /* update in search service */
//        searchServiceGateway.update(database);
        log.info("Refreshed view with id {}", view.getId());
    }

    @Override
    @Transactional
    public View create(Database database, String ownedBy, CreateViewDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, ColumnNotFoundException {
        /* create in metadata database */
        final View view = View.builder()
                .database(database)
                .name(data.getName())
                .internalName(metadataMapper.nameToInternalName(data.getName()))
                .ownedBy(ownedBy)
                .identifiers(new LinkedList<>())
                .columns(new LinkedList<>())
                .isInitialView(false)
                .isSchemaPublic(data.getIsSchemaPublic())
                .isPublic(data.getIsPublic())
                .build();
        /* create in data service */
        data.setName(view.getInternalName());
        final ViewDto rawView = dataServiceGateway.createView(database.getId(), data);
        view.setColumns(rawView.getColumns()
                .stream()
                .map(metadataMapper::viewColumnDtoToViewColumn)
                .toList());
        view.getColumns()
                .forEach(column -> column.setView(view));
        view.setQuery(rawView.getQuery());
        view.setQueryHash(Hashing.sha256()
                .hashString(rawView.getQuery(), StandardCharsets.UTF_8)
                .toString());
        database.getViews()
                .add(view);
        database = databaseRepository.save(database);
        final Optional<View> optional = database.getViews()
                .stream()
                .filter(v -> v.getInternalName().equals(view.getInternalName()))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find created view");
            throw new MalformedException("Failed to find created view");
        }
        /* update cache */
        databaseCacheRepository.deleteById(view.getDatabase().getId());
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Created view with id {}", optional.get().getId());
        return optional.get();
    }

    @Override
    @Transactional
    public View update(View view, ViewUpdateDto data) throws DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {
        view.setIsPublic(data.getIsPublic());
        view.setIsSchemaPublic(data.getIsSchemaPublic());
        final Database database = databaseRepository.save(view.getDatabase());
        /* update cache */
        databaseCacheRepository.deleteById(view.getDatabase().getId());
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Updated view with id {}", view.getId());
        return view;
    }

}
