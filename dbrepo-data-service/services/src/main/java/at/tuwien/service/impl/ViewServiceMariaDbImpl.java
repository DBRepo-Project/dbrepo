package at.tuwien.service.impl;

import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.ViewMalformedException;
import at.tuwien.exception.ViewNotFoundException;
import at.tuwien.mapper.DataMapper;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.SchemaService;
import at.tuwien.service.ViewService;
import com.google.common.hash.Hashing;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@Log4j2
@Service
public class ViewServiceMariaDbImpl extends HibernateConnector implements ViewService {

    private final DataMapper dataMapper;
    private final QueryConfig queryConfig;
    private final SchemaService schemaService;
    private final MariaDbMapper mariaDbMapper;
    private final MetadataMapper metadataMapper;

    @Autowired
    public ViewServiceMariaDbImpl(DataMapper dataMapper, QueryConfig queryConfig, SchemaService schemaService,
                                  MariaDbMapper mariaDbMapper, MetadataMapper metadataMapper) {
        this.dataMapper = dataMapper;
        this.queryConfig = queryConfig;
        this.schemaService = schemaService;
        this.mariaDbMapper = mariaDbMapper;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public Boolean existsByName(PrivilegedDatabaseDto database, String name) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        final Boolean queryResult;
        try {
            /* find view data */
            final long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.selectExistsTableOrViewRawQuery());
            statement.setString(1, database.getInternalName());
            statement.setString(2, name);
            final ResultSet resultSet = statement.executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            queryResult = mariaDbMapper.resultSetToBoolean(resultSet);
        } catch (SQLException e) {
            log.error("Failed to prepare statement {}", e.getMessage());
            throw new QueryMalformedException("Failed to prepare statement: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        return queryResult;
    }

    @Override
    public List<ViewDto> getSchemas(PrivilegedDatabaseDto database) throws SQLException, DatabaseMalformedException,
            ViewNotFoundException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        final List<ViewDto> views = new LinkedList<>();
        try {
            /* inspect tables before views */
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.databaseViewsSelectRawQuery());
            statement.setString(1, database.getInternalName());
            final long start = System.currentTimeMillis();
            final ResultSet resultSet1 = statement.executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            while (resultSet1.next()) {
                final String viewName = resultSet1.getString(1);
                if (viewName.length() == 64) {
                    log.trace("view {}.{} seems to be a subset view (name length = 64), skip.", database.getInternalName(), viewName);
                    continue;
                }
                if (database.getViews().stream().anyMatch(v -> v.getInternalName().equals(viewName))) {
                    log.trace("view {}.{} already known to metadata database, skip.", database.getInternalName(), viewName);
                    continue;
                }
                final ViewDto view;
                view = schemaService.inspectView(database, viewName);
                if (database.getTables().stream().noneMatch(t -> t.getInternalName().equals(view.getInternalName()))) {
                    views.add(view);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get view schemas: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to get view schemas: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Found {} view schema(s)", views.size());
        return views;
    }

    @Override
    public ViewDto create(PrivilegedDatabaseDto database, QueryDto subset) throws ViewMalformedException,
            SQLException {
        final ViewCreateDto data = ViewCreateDto.builder()
                .name(metadataMapper.queryDtoToViewName(subset))
                .query(subset.getQuery())
                .isPublic(false)
                .build();
        return create(database, data);
    }

    @Override
    public ViewDto create(PrivilegedDatabaseDto database, ViewCreateDto data) throws SQLException,
            ViewMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        ViewDto view = ViewDto.builder()
                .name(data.getName())
                .internalName(mariaDbMapper.nameToInternalName(data.getName()))
                .query(data.getQuery())
                .queryHash(Hashing.sha256()
                        .hashString(data.getQuery(), StandardCharsets.UTF_8)
                        .toString())
                .isPublic(database.getIsPublic())
                .creator(database.getOwner())
                .createdBy(database.getOwner().getId())
                .identifiers(new LinkedList<>())
                .isInitialView(false)
                .vdbid(database.getId())
                .database(metadataMapper.privilegedDatabaseDtoToDatabaseDto(database))
                .columns(new LinkedList<>())
                .build();
        try {
            /* create view if not exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.viewCreateRawQuery(view.getInternalName(), data.getQuery()))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            /* select view columns */
            final PreparedStatement statement2 = connection.prepareStatement(mariaDbMapper.databaseTableColumnsSelectRawQuery());
            statement2.setString(1, database.getInternalName());
            statement2.setString(2, view.getInternalName());
            final ResultSet resultSet2 = statement2.executeQuery();
            while (resultSet2.next()) {
                view = dataMapper.resultSetToTable(resultSet2, view, queryConfig);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to create view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created view with name {}", view.getName());
        return view;
    }

    @Override
    public void delete(PrivilegedDatabaseDto database, String viewName) throws SQLException, ViewMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* drop view if exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.dropViewRawQuery(viewName))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to delete view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted view {}.{}", database.getInternalName(), viewName);
    }


    @Override
    public Long count(PrivilegedViewDto view, Instant timestamp) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(view.getDatabase());
        final Connection connection = dataSource.getConnection();
        final Long queryResult;
        try {
            /* find view data */
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectCountRawQuery(
                            view.getDatabase().getInternalName(), view.getInternalName(), timestamp))
                    .executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            queryResult = mariaDbMapper.resultSetToNumber(resultSet);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to find row count from view {}.{}: {}", view.getDatabase().getInternalName(), view.getInternalName(), e.getMessage());
            throw new QueryMalformedException("Failed to find row count from view " + view.getDatabase().getInternalName() + "." + view.getInternalName() + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find row count from view {}.{}", view.getDatabase().getInternalName(), view.getInternalName());
        return queryResult;
    }

}
