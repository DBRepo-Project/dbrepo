package at.tuwien.service.impl;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.CredentialService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Log4j2
@Service
public class CredentialServiceImpl implements CredentialService {

    private final MetadataServiceGateway gateway;
    private final Cache<UUID, UserDto> userCache;
    private final Cache<UUID, ViewDto> viewCache;
    private final Cache<UUID, DatabaseAccessDto> accessCache;
    private final Cache<UUID, TableDto> tableCache;
    private final Cache<UUID, DatabaseDto> databaseCache;
    private final Cache<UUID, ContainerDto> containerCache;

    @Autowired
    public CredentialServiceImpl(MetadataServiceGateway gateway, Cache<UUID, UserDto> userCache,
                                 Cache<UUID, ViewDto> viewCache, Cache<UUID, DatabaseAccessDto> accessCache,
                                 Cache<UUID, TableDto> tableCache,
                                 Cache<UUID, DatabaseDto> databaseCache,
                                 Cache<UUID, ContainerDto> containerCache) {
        this.gateway = gateway;
        this.userCache = userCache;
        this.viewCache = viewCache;
        this.accessCache = accessCache;
        this.tableCache = tableCache;
        this.databaseCache = databaseCache;
        this.containerCache = containerCache;
    }

    @Override
    public DatabaseDto getDatabase(UUID id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final DatabaseDto cacheDatabase = databaseCache.getIfPresent(id);
        if (cacheDatabase != null) {
            log.trace("found database with id {} in cache", id);
            return cacheDatabase;
        }
        log.debug("database with id {} not it cache (anymore): reload from metadata service", id);
        final DatabaseDto database = gateway.getDatabaseById(id);
        databaseCache.put(id, database);
        return database;
    }

    @Override
    public TableDto getTable(UUID databaseId, UUID tableId) throws RemoteUnavailableException,
            MetadataServiceException, TableNotFoundException {
        final TableDto cacheTable = tableCache.getIfPresent(tableId);
        if (cacheTable != null) {
            log.trace("found table with id {} in cache", tableId);
            return cacheTable;
        }
        log.debug("table with id {} not it cache (anymore): reload from metadata service", tableId);
        final TableDto table = gateway.getTableById(databaseId, tableId);
        tableCache.put(tableId, table);
        return table;
    }

    @Override
    public void invalidateAccess(UUID databaseId) {
        accessCache.invalidate(databaseId);
        log.debug("invalidated access for database with id {} in cache", databaseId);
    }

    @Override
    public ContainerDto getContainer(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            ContainerNotFoundException {
        final ContainerDto cacheContainer = containerCache.getIfPresent(id);
        if (cacheContainer != null) {
            log.trace("found container with id {} in cache", id);
            return cacheContainer;
        }
        log.debug("container with id {} not it cache (anymore): reload from metadata service", id);
        final ContainerDto container = gateway.getContainerById(id);
        containerCache.put(id, container);
        return container;
    }

    @Override
    public ViewDto getView(UUID databaseId, UUID viewId) throws RemoteUnavailableException,
            MetadataServiceException, ViewNotFoundException {
        final ViewDto cacheView = viewCache.getIfPresent(viewId);
        if (cacheView != null) {
            log.trace("found view with id {} in cache", viewId);
            return cacheView;
        }
        log.debug("view with id {} not it cache (anymore): reload from metadata service", viewId);
        final ViewDto view = gateway.getViewById(databaseId, viewId);
        viewCache.put(viewId, view);
        return view;
    }

    @Override
    public UserDto getUser(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException {
        final UserDto cacheUser = userCache.getIfPresent(id);
        if (cacheUser != null) {
            log.trace("found user with id {} in cache", id);
            return cacheUser;
        }
        log.debug("user with id {} not it cache (anymore): reload from metadata service", id);
        final UserDto user = gateway.getUserById(id);
        userCache.put(id, user);
        return user;
    }

    @Override
    public DatabaseAccessDto getAccess(UUID databaseId, UUID userId) throws RemoteUnavailableException,
            MetadataServiceException, NotAllowedException {
        final DatabaseAccessDto cacheAccess = accessCache.getIfPresent(databaseId);
        if (cacheAccess != null) {
            log.trace("found access for user with id {} to database with id {} in cache", userId, databaseId);
            return cacheAccess;
        }
        log.debug("access for user with id {} to database with id {} not it cache (anymore): reload from metadata service", userId, databaseId);
        final DatabaseAccessDto access = gateway.getAccess(databaseId, userId);
        accessCache.put(databaseId, access);
        return access;
    }

    /**
     * Method for test cases to remove all caches.
     */
    public void invalidateAll() {
        userCache.invalidateAll();
        viewCache.invalidateAll();
        accessCache.invalidateAll();
        tableCache.invalidateAll();
        databaseCache.invalidateAll();
        containerCache.invalidateAll();
    }

}
