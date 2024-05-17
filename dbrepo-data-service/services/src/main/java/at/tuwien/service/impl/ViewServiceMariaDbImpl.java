package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.service.StorageService;
import at.tuwien.service.ViewService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

@Log4j2
@Service
public class ViewServiceMariaDbImpl extends HibernateConnector implements ViewService {

    private final MariaDbMapper mariaDbMapper;
    private final StorageService storageService;
    private final DataDatabaseSidecarGateway dataDatabaseSidecarGateway;

    @Autowired
    public ViewServiceMariaDbImpl(MariaDbMapper mariaDbMapper, StorageService storageService,
                                  DataDatabaseSidecarGateway dataDatabaseSidecarGateway) {
        this.mariaDbMapper = mariaDbMapper;
        this.storageService = storageService;
        this.dataDatabaseSidecarGateway = dataDatabaseSidecarGateway;
    }

    @Override
    public void create(PrivilegedDatabaseDto database, ViewCreateDto data) throws SQLException,
            ViewMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create view if not exists */
            connection.prepareStatement("CREATE VIEW IF NOT EXISTS `" + data.getName() + "` AS (" + data.getQuery() + ")")
                    .execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create view: {}", e.getMessage());
            throw new ViewMalformedException("Failed to create view: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created view with name {}", data.getName());
    }

    @Override
    public QueryResultDto data(PrivilegedViewDto view, Instant timestamp, Long page, Long size) throws SQLException,
            ViewMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(view.getDatabase());
        final Connection connection = dataSource.getConnection();
        final QueryResultDto queryResult;
        try {
            /* find table data */
            final ResultSet resultSet = connection.prepareStatement(
                            mariaDbMapper.selectDatasetRawQuery(view.getDatabase().getInternalName(), view.getInternalName(),
                                    view.getColumns(), timestamp, size, page))
                    .executeQuery();
            queryResult = mariaDbMapper.resultListToQueryResultDto(view.getColumns(), resultSet);
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
            connection.prepareStatement("DROP VIEW IF EXISTS `" + view.getInternalName() + "`;")
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
    @Transactional
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
            throws SQLException, QueryMalformedException, SidecarExportException, StorageNotFoundException,
            StorageUnavailableException {
        final String filename = RandomStringUtils.randomAlphabetic(40) + ".csv";
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* export to data database sidecar */
            connection.prepareStatement(mariaDbMapper.tableOrViewToRawExportQuery(database.getInternalName(),
                            view.getInternalName(), view.getColumns(), timestamp, filename))
                    .executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        dataDatabaseSidecarGateway.exportFile(database.getContainer().getSidecarHost(), database.getContainer().getSidecarPort(), filename);
        return storageService.getResource(filename);
    }

}
