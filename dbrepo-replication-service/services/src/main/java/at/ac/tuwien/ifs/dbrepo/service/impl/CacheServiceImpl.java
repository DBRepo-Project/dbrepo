package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

    private final TableService tableService;
    private final MetadataServiceGateway gateway;
    private final Cache<String, UserDto> userCache;
    private final Cache<UUID, ViewDto> viewCache;
    private final Cache<UUID, ImageDto> imageCache;
    private final Cache<UUID, TableDto> tableCache;
    private final Cache<UUID, DatabaseDto> databaseCache;
    private final Cache<UUID, ContainerDto> containerCache;
    private final Cache<UUID, DatabaseAccessDto> accessCache;
    private final Cache<UUID, TableStatisticDto> statisticCache;

    @Autowired
    public CacheServiceImpl(TableService tableService, MetadataServiceGateway gateway, Cache<String, UserDto> userCache,
                            Cache<UUID, ViewDto> viewCache, Cache<UUID, ImageDto> imageCache,
                            Cache<UUID, TableDto> tableCache, Cache<UUID, DatabaseAccessDto> accessCache,
                            Cache<UUID, DatabaseDto> databaseCache, Cache<UUID, ContainerDto> containerCache,
                            Cache<UUID, TableStatisticDto> statisticCache) {
        this.tableService = tableService;
        this.gateway = gateway;
        this.userCache = userCache;
        this.viewCache = viewCache;
        this.imageCache = imageCache;
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
                log.atTrace()
                        .setMessage("found database with id " + id)
                        .addKeyValue("cache_hit", true)
                        .log();
                return cacheDatabase;
            }
            log.atTrace()
                    .setMessage("reload database from metadata service with id " + id)
                    .addKeyValue("cache_hit", false)
                    .log();
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
            log.atTrace()
                    .setMessage("found table with id " + tableId)
                    .addKeyValue("cache_hit", true)
                    .log();
            return cacheTable;
        }
        log.atTrace()
                .setMessage("reload table from metadata service with id " + tableId)
                .addKeyValue("cache_hit", false)
                .log();
        final TableDto table = gateway.getTableById(databaseId, tableId);
        tableCache.put(tableId, table);
        return table;
    }

    @Override
    public TableStatisticDto getStatistic(DatabaseDto database, ViewDto view) throws TableNotFoundException,
            TableMalformedException, QueryMalformedException, SQLException {
        final TableStatisticDto cacheStatistic = statisticCache.getIfPresent(view.getId());
        if (cacheStatistic != null) {
            log.atTrace()
                    .setMessage("found view with id " + view.getId())
                    .addKeyValue("cache_hit", true)
                    .log();
            return cacheStatistic;
        }
        log.atTrace()
                .setMessage("reload view from metadata service with id " + view.getId())
                .addKeyValue("cache_hit", false)
                .log();
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
            log.atTrace()
                    .setMessage("found container with id " + id)
                    .addKeyValue("cache_hit", true)
                    .log();
            return cacheContainer;
        }
        log.atTrace()
                .setMessage("reload container from metadata service with id " + id)
                .addKeyValue("cache_hit", false)
                .log();
        final ContainerDto container = gateway.getContainerById(id);
        containerCache.put(id, container);
        return container;
    }

    @Override
    public ImageDto getImage(UUID id) throws RemoteUnavailableException, MetadataServiceException,
            ImageNotFoundException {
        final ImageDto cacheImage = imageCache.getIfPresent(id);
        if (cacheImage != null) {
            log.atTrace()
                    .setMessage("found image with id " + id)
                    .addKeyValue("cache_hit", true)
                    .log();
            return cacheImage;
        }
        log.atTrace()
                .setMessage("reload cacheImage from metadata service with id " + id)
                .addKeyValue("cache_hit", false)
                .log();
        final ImageDto image = gateway.getImageById(id);
        imageCache.put(id, image);
        return image;
    }

    @Override
    public ViewDto getView(UUID databaseId, UUID viewId) throws RemoteUnavailableException,
            MetadataServiceException, ViewNotFoundException {
        final ViewDto cacheView = viewCache.getIfPresent(viewId);
        if (cacheView != null) {
            log.atTrace()
                    .setMessage("found view with id " + viewId)
                    .addKeyValue("cache_hit", true)
                    .log();
            return cacheView;
        }
        log.atTrace()
                .setMessage("reload view from metadata service with id " + viewId)
                .addKeyValue("cache_hit", false)
                .log();
        final ViewDto view = gateway.getViewById(databaseId, viewId);
        viewCache.put(viewId, view);
        return view;
    }

    @Override
    public UserDto getUser(String username) throws RemoteUnavailableException, MetadataServiceException,
            UserNotFoundException {
        final UserDto cacheUser = userCache.getIfPresent(username);
        if (cacheUser != null) {
            log.atTrace()
                    .setMessage("found user " + username)
                    .addKeyValue("cache_hit", true)
                    .log();
            return cacheUser;
        }
        log.atTrace()
                .setMessage("reload user from metadata service " + username)
                .addKeyValue("cache_hit", false)
                .log();
        final UserDto user = gateway.getUserByUsername(username);
        userCache.put(username, user);
        return user;
    }

    @Override
    public DatabaseAccessDto getAccess(UUID databaseId, String username) throws RemoteUnavailableException,
            MetadataServiceException, NotAllowedException {
        final DatabaseAccessDto cacheAccess = accessCache.getIfPresent(databaseId);
        if (cacheAccess != null) {
            log.atTrace()
                    .setMessage("found access for user " + username)
                    .addKeyValue("cache_hit", true)
                    .log();
            return cacheAccess;
        }
        log.atTrace()
                .setMessage("reload access from metadata service for user " + username)
                .addKeyValue("cache_hit", false)
                .log();
        final DatabaseAccessDto access = gateway.getAccess(databaseId, username);
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
        imageCache.invalidateAll();
        statisticCache.invalidateAll();
    }

}
