package at.tuwien.service.impl;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class AccessServiceImpl implements AccessService {

    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final DatabaseRepository databaseRepository;
    private final DataServiceGateway dataServiceGateway;
    private final SearchServiceGateway searchServiceGateway;

    @Autowired
    public AccessServiceImpl(MetadataMapper metadataMapper, DatabaseService databaseService,
                             DatabaseRepository databaseRepository, DataServiceGateway dataServiceGateway,
                             SearchServiceGateway searchServiceGateway) {
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.databaseRepository = databaseRepository;
        this.dataServiceGateway = dataServiceGateway;
        this.searchServiceGateway = searchServiceGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabaseAccess> list(Database database) {
        return database.getAccesses();
    }

    @Override
    @Transactional(readOnly = true)
    public DatabaseAccess find(Database database, User user) throws AccessNotFoundException {
        final Optional<DatabaseAccess> optional = database.getAccesses()
                .stream()
                .filter(a -> a.getUser().getUsername().equals(user.getUsername()))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find database access for database with id: {}", database.getId());
            throw new AccessNotFoundException("Failed to find database access for database with id: " + database.getId());
        }
        return optional.get();
    }

    @Override
    @Transactional
    public DatabaseAccess create(Database database, User user, AccessTypeDto type) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        /* create in data database */
        dataServiceGateway.createAccess(database.getId(), user.getId(), type);
        /* create in metadata database */
        final DatabaseAccess access = DatabaseAccess.builder()
                .hdbid(database.getId())
                .database(database)
                .huserid(user.getId())
                .type(metadataMapper.accessTypeDtoToAccessType(type))
                .user(user)
                .build();
        database.getAccesses()
                .add(access);
        database = databaseRepository.save(database);
        /* create in search service */
        searchServiceGateway.update(database);
        log.info("Created access to database with id {}", database.getId());
        return access;
    }

    @Override
    @Transactional
    public void update(Database database, User user, AccessTypeDto access) throws DataServiceException,
            DataServiceConnectionException, AccessNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        /* update in data database */
        dataServiceGateway.updateAccess(database.getId(), user.getId(), access);
        /* update in metadata database */
        final Optional<DatabaseAccess> optional = database.getAccesses()
                .stream()
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to update access for user with id: {}", user.getId());
            throw new AccessNotFoundException("Failed to find update access for user with id: " + user.getId());
        }
        optional.get()
                .setType(metadataMapper.accessTypeDtoToAccessType(access));
        database = databaseRepository.save(database);
        /* update in search service */
        searchServiceGateway.update(database);
        log.info("Updated access to database with id {}", database.getId());
    }

    @Override
    @Transactional
    public void delete(Database database, User user) throws AccessNotFoundException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        /* delete in data database */
        dataServiceGateway.deleteAccess(database.getId(), user.getId());
        /* delete in metadata database */
        database.getAccesses().remove(find(database, user));
        databaseRepository.save(database);
        /* update in search service */
        searchServiceGateway.update(databaseService.findById(database.getId()));
        log.info("Deleted access to database with id {}", database.getId());
    }

}
