package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.SortTypeDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataDatabaseSidecarGateway;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.QueryService;
import at.tuwien.service.StorageService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class QueryServiceMariaDbImpl extends HibernateConnector implements QueryService {

    private final MariaDbMapper mariaDbMapper;
    private final MetadataMapper metadataMapper;
    private final StorageService storageService;
    private final DataDatabaseSidecarGateway dataDatabaseSidecarGateway;

    @Autowired
    public QueryServiceMariaDbImpl(MariaDbMapper mariaDbMapper, MetadataMapper metadataMapper,
                                   StorageService storageService,
                                   DataDatabaseSidecarGateway dataDatabaseSidecarGateway) {
        this.mariaDbMapper = mariaDbMapper;
        this.metadataMapper = metadataMapper;
        this.storageService = storageService;
        this.dataDatabaseSidecarGateway = dataDatabaseSidecarGateway;
    }

    @Override
    public void createQueryStore(PrivilegedContainerDto container, String databaseName) throws SQLException, QueryStoreCreateException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container, databaseName);
        final Connection connection = dataSource.getConnection();
        try {
            /* create query store */
            connection.prepareStatement("CREATE SEQUENCE `qs_queries_seq` NOCACHE;")
                    .execute();
            connection.prepareStatement("CREATE TABLE `qs_queries` ( `id` bigint not null primary key default nextval(`qs_queries_seq`), `created` datetime not null default now(), `executed` datetime not null default now(), `created_by` varchar(36) not null, `query` text not null, `query_normalized` text not null, `is_persisted` boolean not null, `query_hash` varchar(255) not null, `result_hash` varchar(255), `result_number` bigint );")
                    .execute();
            connection.prepareStatement("CREATE PROCEDURE hash_table(IN name VARCHAR(255), OUT hash VARCHAR(255), OUT count BIGINT) BEGIN DECLARE _sql TEXT; SELECT CONCAT('SELECT SHA2(GROUP_CONCAT(CONCAT_WS(\\'\\',', GROUP_CONCAT(CONCAT('`', column_name, '`') ORDER BY column_name), ') SEPARATOR \\',\\'), 256) AS hash, COUNT(*) AS count FROM `', name, '` INTO @hash, @count;') FROM `information_schema`.`columns` WHERE `table_schema` = DATABASE() AND `table_name` = name INTO _sql; PREPARE stmt FROM _sql; EXECUTE stmt; DEALLOCATE PREPARE stmt; SET hash = @hash; SET count = @count; END;")
                    .execute();
            connection.prepareStatement("CREATE PROCEDURE store_query(IN query TEXT, IN executed DATETIME, OUT queryId BIGINT) BEGIN DECLARE _queryhash varchar(255) DEFAULT SHA2(query, 256); DECLARE _username varchar(255) DEFAULT REGEXP_REPLACE(current_user(), '@.*', ''); DECLARE _query TEXT DEFAULT CONCAT('CREATE OR REPLACE TABLE _tmp AS (', query, ')'); PREPARE stmt FROM _query; EXECUTE stmt; DEALLOCATE PREPARE stmt; CALL hash_table('_tmp', @hash, @count); DROP TABLE IF EXISTS `_tmp`; IF @hash IS NULL THEN INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); ELSE INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); END IF; END;")
                    .execute();
            connection.prepareStatement("CREATE DEFINER = 'root' PROCEDURE _store_query(IN _username VARCHAR(255), IN query TEXT, IN executed DATETIME, OUT queryId BIGINT) BEGIN DECLARE _queryhash varchar(255) DEFAULT SHA2(query, 256); DECLARE _query TEXT DEFAULT CONCAT('CREATE OR REPLACE TABLE _tmp AS (', query, ')'); PREPARE stmt FROM _query; EXECUTE stmt; DEALLOCATE PREPARE stmt; CALL hash_table('_tmp', @hash, @count); DROP TABLE IF EXISTS `_tmp`; IF @hash IS NULL THEN INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); ELSE INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); END IF; END;")
                    .execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create query store: {}", e.getMessage());
            throw new QueryStoreCreateException("Failed to create query store: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created query store in database with name {}", databaseName);
    }

    @Override
    public QueryResultDto execute(PrivilegedDatabaseDto database, ExecuteStatementDto metadata, UUID userId, Long page,
                                  Long size, SortTypeDto sortDirection, String sortColumn)
            throws QueryStoreInsertException, SQLException, QueryNotFoundException, TableMalformedException {
        final Long queryId = storeQuery(database, metadata, userId);
        final QueryDto query = findById(database, queryId);
        return reExecute(database, query, page, size, sortDirection, sortColumn);
    }

    @Override
    public QueryResultDto reExecute(PrivilegedDatabaseDto database, QueryDto query, Long page, Long size,
                                    SortTypeDto sortDirection, String sortColumn) throws TableMalformedException,
            SQLException {
        final List<ColumnDto> columns;
        try {
            columns = mariaDbMapper.parseColumns(metadataMapper.privilegedDatabaseDtoToDatabaseDto(database), query.getQuery());
        } catch (JSQLParserException e) {
            log.error("Failed to map/parse columns: {}", e.getMessage());
            throw new TableMalformedException("Failed to map/parse columns: " + e.getMessage(), e);
        }
        final String statement = mariaDbMapper.selectRawSelectQuery(query.getQuery(), query.getExecution(), page, size);
        final QueryResultDto dto = executeNonPersistent(database, statement, columns);
        dto.setId(query.getId());
        return dto;
    }

    @Override
    public Long reExecuteCount(PrivilegedDatabaseDto database, QueryDto query) throws TableMalformedException,
            SQLException, QueryMalformedException {
        final String statement = mariaDbMapper.countRawSelectQuery(query.getQuery(), query.getExecution());
        return executeCountNonPersistent(database, statement, query.getExecution());
    }

    @Override
    public List<QueryDto> findAll(PrivilegedDatabaseDto database, Boolean filterPersisted) throws SQLException,
            QueryNotFoundException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final PreparedStatement statement = connection.prepareStatement("SELECT `id`, `created`, `created_by`, `query`, `query_hash`, `result_hash`, `result_number`, `is_persisted` FROM `qs_queries`" + filterPersisted != null ? " WHERE `is_persisted` = ?;" : ";");
            if (filterPersisted != null) {
                statement.setBoolean(1, filterPersisted);
            }
            final ResultSet resultSet = statement.getResultSet();
            final List<QueryDto> queries = new LinkedList<>();
            while (resultSet.next()) {
                queries.add(mariaDbMapper.resultSetToQueryDto(resultSet));
            }
            log.info("Find {} queries", queries.size());
            return queries;
        } catch (SQLException e) {
            log.error("Failed to find queries: {}", e.getMessage());
            throw new QueryNotFoundException("Failed to find queries: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public ExportResourceDto export(PrivilegedDatabaseDto database, QueryDto query, Instant timestamp, String filename)
            throws SQLException, QueryMalformedException, SidecarExportException, StorageNotFoundException,
            StorageUnavailableException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* export to data database sidecar */
            final List<ColumnDto> columns = mariaDbMapper.parseColumns(metadataMapper.privilegedDatabaseDtoToDatabaseDto(database), query.getQuery());
            connection.prepareStatement(mariaDbMapper.subsetToRawExportQuery(query.getQuery(), columns, timestamp, filename))
                    .executeUpdate();
            connection.commit();
        } catch (SQLException | JSQLParserException e) {
            connection.rollback();
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        dataDatabaseSidecarGateway.exportFile(database.getContainer().getSidecarHost(), database.getContainer().getSidecarPort(), filename);
        return storageService.getResource(filename);
    }

    public QueryResultDto executeNonPersistent(PrivilegedDatabaseDto database, String statement,
                                               List<ColumnDto> columns) throws SQLException, TableMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final PreparedStatement preparedStatement = connection.prepareStatement(statement);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return mariaDbMapper.resultListToQueryResultDto(columns, resultSet);
        } catch (SQLException e) {
            log.error("Failed to execute and map time-versioned query: {}", e.getMessage());
            throw new TableMalformedException("Failed to execute and map time-versioned query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public Long executeCountNonPersistent(PrivilegedDatabaseDto database, String statement, Instant timestamp)
            throws SQLException, QueryMalformedException, TableMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.countRawSelectQuery(statement, timestamp))
                    .executeQuery();
            return mariaDbMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new TableMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public QueryDto findById(PrivilegedDatabaseDto database, Long queryId) throws SQLException, QueryNotFoundException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final PreparedStatement preparedStatement = connection.prepareStatement("SELECT `id`, `created`, `created_by`, `query`, `query_hash`, `result_hash`, `result_number`, `is_persisted` FROM `qs_queries` q WHERE q.`id` = ?");
            preparedStatement.setLong(1, queryId);
            return mariaDbMapper.resultSetToQueryDto(preparedStatement.executeQuery());
        } catch (SQLException e) {
            log.error("Failed to find query with id {}: {}", queryId, e.getMessage());
            throw new QueryNotFoundException("Failed to find query with id " + queryId + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public Long storeQuery(PrivilegedDatabaseDto database, ExecuteStatementDto metadata, UUID userId) throws SQLException,
            QueryStoreInsertException {
        /* save */
        final Long queryId;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* insert query into query store */
            final CallableStatement callableStatement = connection.prepareCall("{call _store_query(?, ?, ?, ?)}");
            callableStatement.setString(1, String.valueOf(userId));
            callableStatement.setString(2, metadata.getStatement());
            callableStatement.setTimestamp(3, Timestamp.from(metadata.getTimestamp()));
            callableStatement.registerOutParameter(4, Types.BIGINT);
            callableStatement.executeUpdate();
            queryId = callableStatement.getLong(4);
            callableStatement.close();
            log.info("Stored query with id {} in database with name {}", queryId, database.getInternalName());
            connection.commit();
            return queryId;
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to store query: {}", e.getMessage());
            throw new QueryStoreInsertException("Failed to store query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public void persist(PrivilegedDatabaseDto database, Long queryId, Boolean persist) throws SQLException,
            QueryStorePersistException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* update query */
            final PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `qs_queries` SET `is_persisted` = ? WHERE `id` = ?");
            preparedStatement.setLong(1, queryId);
            preparedStatement.setBoolean(2, persist);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to (un-)persist query: {}", e.getMessage());
            throw new QueryStorePersistException("Failed to (un-)persist query", e);
        } finally {
            dataSource.close();
        }
        log.info("Performed (un-)persist for query with id {} in database with name {}", queryId, database.getInternalName());
    }

    @Override
    public void deleteStaleQueries(PrivilegedDatabaseDto database) throws SQLException, QueryStoreGCException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            connection.prepareStatement("DELETE FROM `qs_queries` WHERE `is_persisted` = false AND ABS(DATEDIFF(`created`, NOW())) >= 1")
                    .executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete stale queries: {}", e.getMessage());
            throw new QueryStoreGCException("Failed to delete stale queries: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

}
