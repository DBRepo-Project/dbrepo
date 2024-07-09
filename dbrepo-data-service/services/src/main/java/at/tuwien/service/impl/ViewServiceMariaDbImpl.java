package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.config.QueryConfig;
import at.tuwien.config.S3Config;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import at.tuwien.mapper.DataMapper;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.SchemaService;
import at.tuwien.service.StorageService;
import at.tuwien.service.ViewService;
import com.google.common.hash.Hashing;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
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

    private final S3Config s3Config;
    private final DataMapper dataMapper;
    private final QueryConfig queryConfig;
    private final MariaDbMapper mariaDbMapper;
    private final SchemaService schemaService;
    private final StorageService storageService;
    private final MetadataMapper metadataMapper;
    private final DataDatabaseSidecarGateway dataDatabaseSidecarGateway;

    @Autowired
    public ViewServiceMariaDbImpl(S3Config s3Config, DataMapper dataMapper, QueryConfig queryConfig,
                                  MariaDbMapper mariaDbMapper, SchemaService schemaService,
                                  StorageService storageService, MetadataMapper metadataMapper,
                                  DataDatabaseSidecarGateway dataDatabaseSidecarGateway) {
        this.s3Config = s3Config;
        this.dataMapper = dataMapper;
        this.queryConfig = queryConfig;
        this.mariaDbMapper = mariaDbMapper;
        this.schemaService = schemaService;
        this.storageService = storageService;
        this.metadataMapper = metadataMapper;
        this.dataDatabaseSidecarGateway = dataDatabaseSidecarGateway;
    }

    @Override
    public List<ViewDto> getSchemas(PrivilegedDatabaseDto database) throws SQLException, DatabaseMalformedException,
            ViewMalformedException, ViewNotFoundException, ViewSchemaException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        final List<ViewDto> views = new LinkedList<>();
        try {
            /* inspect tables before views */
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.databaseViewsSelectRawQuery());
            statement.setString(1, database.getInternalName());
            final ResultSet resultSet1 = statement.executeQuery();
            while (resultSet1.next()) {
                final String viewName = resultSet1.getString(1);
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
            connection.prepareStatement(mariaDbMapper.viewCreateRawQuery(view.getInternalName(), data.getQuery()))
                    .execute();
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
    public QueryResultDto data(PrivilegedViewDto view, Instant timestamp, Long page, Long size) throws SQLException,
            ViewMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(view.getDatabase());
        final Connection connection = dataSource.getConnection();
        final QueryResultDto queryResult;
        try {
            /* find table data */
            final List<ColumnDto> mappedColumns = view.getColumns()
                    .stream()
                    .map(metadataMapper::viewColumnDtoToColumnDto)
                    .toList();
            final ResultSet resultSet = connection.prepareStatement(
                            mariaDbMapper.selectDatasetRawQuery(view.getDatabase().getInternalName(),
                                    view.getInternalName(), mappedColumns, timestamp, size, page))
                    .executeQuery();
            queryResult = dataMapper.resultListToQueryResultDto(mappedColumns, resultSet);
            queryResult.setId(view.getId());
            connection.commit();
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new ViewMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Find data from view {}.{}", view.getDatabase().getInternalName(), view.getInternalName());
        return queryResult;
    }

    @Override
    public void delete(PrivilegedViewDto view) throws SQLException, ViewMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(view.getDatabase());
        final Connection connection = dataSource.getConnection();
        try {
            /* drop view if exists */
            connection.prepareStatement(mariaDbMapper.dropViewRawQuery(view.getInternalName()))
                    .execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to delete view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to delete view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted view {}.{}", view.getDatabase().getInternalName(), view.getInternalName());
    }


    @Override
    public Long count(PrivilegedViewDto view, Instant timestamp) throws SQLException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(view.getDatabase());
        final Connection connection = dataSource.getConnection();
        final Long queryResult;
        try {
            /* find view data */
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectCountRawQuery(
                            view.getDatabase().getInternalName(), view.getInternalName(), timestamp))
                    .executeQuery();
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

    @Override
    public ExportResourceDto exportDataset(PrivilegedDatabaseDto database, ViewDto view, Instant timestamp)
            throws SQLException, QueryMalformedException, StorageNotFoundException, StorageUnavailableException,
            RemoteUnavailableException, SidecarExportException {
        final String fileName = RandomStringUtils.randomAlphabetic(40) + ".csv";
        final String filePath = s3Config.getS3FilePath() + "/" + fileName;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* export to data database sidecar */
            final List<ColumnDto> columns = view.getColumns()
                    .stream()
                    .map(metadataMapper::viewColumnDtoToColumnDto)
                    .toList();
            connection.prepareStatement(mariaDbMapper.tableOrViewToRawExportQuery(database.getInternalName(),
                            view.getInternalName(), columns, timestamp, filePath))
                    .executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        dataDatabaseSidecarGateway.exportFile(database.getContainer().getSidecarHost(),
                database.getContainer().getSidecarPort(), fileName);
        return storageService.getResource(fileName);
    }

}
