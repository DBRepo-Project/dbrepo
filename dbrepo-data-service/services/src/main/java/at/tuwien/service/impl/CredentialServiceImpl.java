package at.tuwien.service.impl;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.user.internal.PrivilegedUserDto;
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
    private final Cache<UUID, PrivilegedUserDto> userCache;
    private final Cache<Long, PrivilegedViewDto> viewCache;
    private final Cache<Long, DatabaseAccessDto> accessCache;
    private final Cache<Long, PrivilegedTableDto> tableCache;
    private final Cache<Long, PrivilegedDatabaseDto> databaseCache;
    private final Cache<Long, PrivilegedContainerDto> containerCache;

    @Autowired
    public CredentialServiceImpl(MetadataServiceGateway gateway, Cache<UUID, PrivilegedUserDto> userCache,
                                 Cache<Long, PrivilegedViewDto> viewCache, Cache<Long, DatabaseAccessDto> accessCache,
                                 Cache<Long, PrivilegedTableDto> tableCache,
                                 Cache<Long, PrivilegedDatabaseDto> databaseCache,
                                 Cache<Long, PrivilegedContainerDto> containerCache) {
        this.gateway = gateway;
        this.userCache = userCache;
        this.viewCache = viewCache;
        this.accessCache = accessCache;
        this.tableCache = tableCache;
        this.databaseCache = databaseCache;
        this.containerCache = containerCache;
    }

    @Override
    public PrivilegedDatabaseDto getDatabase(Long id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final PrivilegedDatabaseDto cacheDatabase = databaseCache.getIfPresent(id);
        if (cacheDatabase != null) {
            log.trace("found database with id {} in cache", id);
            return cacheDatabase;
        }
        log.debug("database with id {} not it cache (anymore): reload from metadata service", id);
        final PrivilegedDatabaseDto database = gateway.getDatabaseById(id);
        databaseCache.put(id, database);
        return database;
    }

    @Override
    public PrivilegedTableDto getTable(Long databaseId, Long tableId) throws RemoteUnavailableException,
            MetadataServiceException, TableNotFoundException {
        final PrivilegedTableDto cacheTable = tableCache.getIfPresent(tableId);
        if (cacheTable != null) {
            log.trace("found table with id {} in cache", tableId);
            return cacheTable;
        }
        log.debug("table with id {} not it cache (anymore): reload from metadata service", tableId);
        final PrivilegedTableDto table = gateway.getTableById(databaseId, tableId);
        tableCache.put(tableId, table);
        return table;
    }

    @Override
    public void invalidateAccess(Long databaseId) {
        accessCache.invalidate(databaseId);
        log.debug("invalidated access for database with id {} in cache", databaseId);
    }

    @Override
    public PrivilegedContainerDto getContainer(Long id) throws RemoteUnavailableException, MetadataServiceException,
            ContainerNotFoundException {
        final PrivilegedContainerDto cacheContainer = containerCache.getIfPresent(id);
        if (cacheContainer != null) {
            log.trace("found container with id {} in cache", id);
            return cacheContainer;
        }
        log.debug("container with id {} not it cache (anymore): reload from metadata service", id);
        final PrivilegedContainerDto container = gateway.getContainerById(id);
        containerCache.put(id, container);
        return container;
    }

    @Override
    public PrivilegedViewDto getView(Long databaseId, Long viewId) throws RemoteUnavailableException,
            MetadataServiceException, ViewNotFoundException {
        final PrivilegedViewDto cacheView = viewCache.getIfPresent(viewId);
        if (cacheView != null) {
            log.trace("found view with id {} in cache", viewId);
            return cacheView;
        }
        log.debug("view with id {} not it cache (anymore): reload from metadata service", viewId);
        final PrivilegedViewDto view = gateway.getViewById(databaseId, viewId);
        viewCache.put(viewId, view);
        return view;
    }

    @Override
    public PrivilegedUserDto getUser(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException {
        final PrivilegedUserDto cacheUser = userCache.getIfPresent(id);
        if (cacheUser != null) {
            log.trace("found user with id {} in cache", id);
            return cacheUser;
        }
        log.debug("user with id {} not it cache (anymore): reload from metadata service", id);
        final PrivilegedUserDto user = gateway.getPrivilegedUserById(id);
        userCache.put(id, user);
        return user;
    }

    @Override
    public DatabaseAccessDto getAccess(Long databaseId, UUID userId) throws RemoteUnavailableException,
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
