package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

@Log4j2
@Service
public class CacheServiceImpl implements CacheService {

    private final TableService tableService;
    private final MetadataServiceGateway gateway;
    private final Cache<UUID, UserDto> userCache;
    private final Cache<UUID, ViewDto> viewCache;
    private final Cache<UUID, TableDto> tableCache;
    private final Cache<UUID, DatabaseDto> databaseCache;
    private final Cache<UUID, ContainerDto> containerCache;
    private final Cache<UUID, DatabaseAccessDto> accessCache;
    private final Cache<UUID, TableStatisticDto> statisticCache;

    @Autowired
    public CacheServiceImpl(TableService tableService, MetadataServiceGateway gateway, Cache<UUID, UserDto> userCache,
                            Cache<UUID, ViewDto> viewCache, Cache<UUID, TableDto> tableCache,
                            Cache<UUID, DatabaseAccessDto> accessCache, Cache<UUID, DatabaseDto> databaseCache,
                            Cache<UUID, ContainerDto> containerCache, Cache<UUID, TableStatisticDto> statisticCache) {
        this.tableService = tableService;
        this.gateway = gateway;
        this.userCache = userCache;
        this.viewCache = viewCache;
        this.tableCache = tableCache;
        this.accessCache = accessCache;
        this.databaseCache = databaseCache;
        this.containerCache = containerCache;
        this.statisticCache = statisticCache;
    }

    @Override
    public DatabaseDto getDatabase(UUID id, boolean forceReload) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        if (!forceReload) {
            final DatabaseDto cacheDatabase = databaseCache.getIfPresent(id);
            if (cacheDatabase != null) {
                log.trace("found database with id {} in cache", id);
                return cacheDatabase;
            }
            log.debug("database with id {} not it cache (anymore): reload from metadata service", id);
        }
        final DatabaseDto database = gateway.getDatabaseById(id);
        databaseCache.put(id, database);
        return database;
    }

    @Override
    public DatabaseDto getDatabase(UUID id) throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        return getDatabase(id, false);
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
    public TableStatisticDto getStatistic(DatabaseDto database, ViewDto view) throws TableNotFoundException,
            TableMalformedException, QueryMalformedException, SQLException {
        final TableStatisticDto cacheStatistic = statisticCache.getIfPresent(view.getId());
        if (cacheStatistic != null) {
            log.trace("found view statistic with id {} in cache", view.getId());
            return cacheStatistic;
        }
        log.debug("view statistic with id {} not it cache (anymore): reload", view.getId());
        final TableStatisticDto statistic = tableService.getStatistics(database, view.getInternalName());
        statistic.setTotalRows(tableService.getCount(database, view.getInternalName(), Instant.now()));
        statisticCache.put(view.getId(), statistic);
        return statistic;
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
        statisticCache.invalidateAll();
    }

}
