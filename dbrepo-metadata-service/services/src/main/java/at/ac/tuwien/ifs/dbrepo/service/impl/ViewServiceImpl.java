package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.ViewCacheRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final ViewCacheRepository viewCacheRepository;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    @Autowired
    public ViewServiceImpl(MetadataMapper metadataMapper, DataServiceGateway dataServiceGateway,
                           DatabaseRepository databaseRepository, SearchServiceGateway searchServiceGateway,
                           DatabaseCacheRepository databaseCacheRepository, ViewCacheRepository viewCacheRepository) {
        this.metadataMapper = metadataMapper;
        this.dataServiceGateway = dataServiceGateway;
        this.databaseRepository = databaseRepository;
        this.searchServiceGateway = searchServiceGateway;
        this.databaseCacheRepository = databaseCacheRepository;
        this.viewCacheRepository = viewCacheRepository;
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
    @Transactional(rollbackFor = Exception.class)
    public void delete(View view) throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, ViewNotFoundException, SearchServiceException, SearchServiceConnectionException {
        if (view.getArchivedAt() == null) {
            view.setArchivedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        }
        // Stored queries may depend on the definition, including through other views.
        final Database database = databaseRepository.save(view.getDatabase());
        final Runnable evict = () -> {
            databaseCacheRepository.deleteById(view.getDatabase().getId());
            viewCacheRepository.deleteById(view.getId());
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict.run();
                }
            });
        } else {
            evict.run();
        }
        searchServiceGateway.update(database);
        log.info("Archived view with id {}", view.getId());
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
                .creationLocation(baseUrl)
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
    public View createReplicated(Database database, String ownedBy, ViewDto data) throws DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DataServiceConnectionException,
            DataServiceException {
        final View view = View.builder()
                .id(data.getId())
                .database(database)
                .name(data.getName())
                .internalName(data.getInternalName())
                .ownedBy(ownedBy)
                .identifiers(new LinkedList<>())
                .columns(new LinkedList<>())
                .isInitialView(Boolean.TRUE.equals(data.getIsInitialView()))
                .isSchemaPublic(data.getIsSchemaPublic())
                .isPublic(data.getIsPublic())
                .creationLocation(data.getCreationLocation())
                .archivedAt(data.getArchivedAt())
                .build();
        final ViewDto rawView = dataServiceGateway.createViewRaw(database.getId(), view.getInternalName(),
                data.getQuery());
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
        final Database saved = databaseRepository.save(database);
        databaseCacheRepository.deleteById(saved.getId());
        searchServiceGateway.update(saved);
        log.info("Created replicated view with id {}", view.getId());
        return view;
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
