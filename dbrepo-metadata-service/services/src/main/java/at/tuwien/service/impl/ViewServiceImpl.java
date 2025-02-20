package at.tuwien.service.impl;

import at.tuwien.api.database.CreateViewDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.ViewUpdateDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.service.ViewService;
import com.google.common.hash.Hashing;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class ViewServiceImpl implements ViewService {

    private final MetadataMapper metadataMapper;
    private final DataServiceGateway dataServiceGateway;
    private final DatabaseRepository databaseRepository;
    private final SearchServiceGateway searchServiceGateway;

    @Autowired
    public ViewServiceImpl(MetadataMapper metadataMapper, DataServiceGateway dataServiceGateway,
                           DatabaseRepository databaseRepository, SearchServiceGateway searchServiceGateway) {
        this.metadataMapper = metadataMapper;
        this.dataServiceGateway = dataServiceGateway;
        this.databaseRepository = databaseRepository;
        this.searchServiceGateway = searchServiceGateway;
    }

    @Override
    public View findById(Database database, UUID viewId) throws ViewNotFoundException {
        final Optional<View> optional = database.getViews()
                .stream()
                .filter(v -> v.getId().equals(viewId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find view with id {}", viewId);
            throw new ViewNotFoundException("Failed to find view with id " + viewId);
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<View> findAll(Database database, User user) {
        if (user == null) {
            return database.getViews()
                    .stream()
                    .filter(View::getIsPublic)
                    .toList();
        }
        return database.getViews()
                .stream()
                .filter(v -> v.getIsPublic() || v.getOwnedBy().equals(user.getId()))
                .toList();
    }

    @Override
    @Transactional
    public void delete(View view) throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            ViewNotFoundException, SearchServiceException, SearchServiceConnectionException {
        /* delete in data service */
        dataServiceGateway.deleteView(view.getDatabase().getId(), view.getId());
        /* delete in metadata database */
        view.getDatabase()
                .getViews()
                .remove(view);
        final Database database = databaseRepository.save(view.getDatabase());
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Deleted view with id {}", view.getId());
    }

    @Override
    @Transactional
    public View create(Database database, User creator, CreateViewDto data) throws MalformedException,
            DataServiceException, DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        /* create in metadata database */
        final View view = View.builder()
                .database(database)
                .name(data.getName())
                .internalName(metadataMapper.nameToInternalName(data.getName()))
                .ownedBy(creator.getId())
                .owner(creator)
                .identifiers(new LinkedList<>())
                .query(data.getQuery())
                .queryHash(Hashing.sha256()
                        .hashString(data.getQuery(), StandardCharsets.UTF_8)
                        .toString())
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
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Created view with id {}", optional.get().getId());
        return optional.get();
    }

    @Override
    @Transactional
    public View update(Database database, View view, ViewUpdateDto data) throws DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException, ViewNotFoundException {
        final Optional<View> optional = database.getViews()
                .stream()
                .filter(v -> v.getInternalName().equals(view.getInternalName()))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find view");
            throw new ViewNotFoundException("Failed to find view");
        }
        final View tmpView = optional.get();
        tmpView.setIsPublic(data.getIsPublic());
        tmpView.setIsSchemaPublic(data.getIsSchemaPublic());
        database = databaseRepository.save(database);
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Updated view with id {}", tmpView.getId());
        return optional.get();
    }

}
