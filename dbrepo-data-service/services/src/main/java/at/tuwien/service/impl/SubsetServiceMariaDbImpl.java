package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.SortTypeDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.mapper.DataMapper;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.SchemaService;
import at.tuwien.service.StorageService;
import at.tuwien.service.SubsetService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.instrument.Counter;
import jakarta.validation.constraints.NotNull;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.commons.lang3.RandomUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.ExtendedAnalysisException;
import org.sparkproject.guava.hash.Hashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Log4j2
@Service
public class SubsetServiceMariaDbImpl extends HibernateConnector implements SubsetService {

    private final Counter httpDataAccessCounter;
    private final DataMapper dataMapper;
    private final SparkSession sparkSession;
    private final MariaDbMapper mariaDbMapper;
    private final SchemaService schemaService;
    private final MetadataMapper metadataMapper;
    private final StorageService storageService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public SubsetServiceMariaDbImpl(Counter httpDataAccessCounter, DataMapper dataMapper, SparkSession sparkSession,
                                    MariaDbMapper mariaDbMapper, SchemaService schemaService,
                                    MetadataMapper metadataMapper, StorageService storageService,
                                    MetadataServiceGateway metadataServiceGateway) {
        this.httpDataAccessCounter = httpDataAccessCounter;
        this.dataMapper = dataMapper;
        this.sparkSession = sparkSession;
        this.mariaDbMapper = mariaDbMapper;
        this.schemaService = schemaService;
        this.metadataMapper = metadataMapper;
        this.storageService = storageService;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @Override
    public void createQueryStore(PrivilegedContainerDto container, String databaseName) throws SQLException,
            QueryStoreCreateException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container, databaseName);
        final Connection connection = dataSource.getConnection();
        try {
            /* create query store */
            long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateSequenceRawQuery())
                    .execute();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateTableRawQuery())
                    .execute();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateHashTableProcedureRawQuery())
                    .execute();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateStoreQueryProcedureRawQuery())
                    .execute();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateInternalStoreQueryProcedureRawQuery())
                    .execute();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
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
    public QueryResultDto execute(PrivilegedDatabaseDto database, String statement, Instant timestamp,
                                  UUID userId, Long page, Long size, SortTypeDto sortDirection, String sortColumn)
            throws QueryStoreInsertException, SQLException, QueryNotFoundException, TableMalformedException,
            UserNotFoundException, NotAllowedException, RemoteUnavailableException, DatabaseNotFoundException,
            MetadataServiceException {
        final Long queryId = storeQuery(database, statement, timestamp, userId);
        final QueryDto query = findById(database, queryId);
        httpDataAccessCounter.increment();
        return reExecute(database, query, page, size, sortDirection, sortColumn);
    }

    @Override
    public QueryResultDto reExecute(PrivilegedDatabaseDto database, QueryDto query, Long page, Long size,
                                    SortTypeDto sortDirection, String sortColumn) throws TableMalformedException,
            SQLException {
        final List<ColumnDto> columns;
        try {
            columns = dataMapper.parseColumns(database.getId(), database.getTables(), query.getQuery());
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
        return executeCountNonPersistent(database, query.getQuery(), query.getExecution());
    }

    @Override
    public List<QueryDto> findAll(PrivilegedDatabaseDto database, Boolean filterPersisted) throws SQLException,
            QueryNotFoundException, RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {
        final List<IdentifierDto> identifiers = metadataServiceGateway.getIdentifiers(database.getId(), null);
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.filterToGetQueriesRawQuery(filterPersisted));
            if (filterPersisted != null) {
                statement.setBoolean(1, filterPersisted);
                log.trace("filter persisted only {}", filterPersisted);
            }
            final ResultSet resultSet = statement.executeQuery();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            final List<QueryDto> queries = new LinkedList<>();
            while (resultSet.next()) {
                final QueryDto query = dataMapper.resultSetToQueryDto(resultSet);
                query.setIdentifiers(identifiers.stream()
                        .filter(i -> i.getType().equals(IdentifierTypeDto.SUBSET))
                        .filter(i -> i.getQueryId().equals(query.getId()))
                        .map(metadataMapper::identifierDtoToIdentifierBriefDto)
                        .toList());
                queries.add(query);
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
    public ExportResourceDto export(PrivilegedDatabaseDto database, QueryDto query, Instant timestamp)
            throws SQLException, QueryMalformedException, StorageNotFoundException, StorageUnavailableException,
            RemoteUnavailableException, ViewNotFoundException {
        final String viewName = "ex_" + Hashing.sha512()
                .hashString(new String(RandomUtils.nextBytes(256), Charset.defaultCharset()), Charset.defaultCharset())
                .toString()
                .substring(0, 60);
        final ExportResourceDto export;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* export to data database sidecar */
            long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.subsetToRawTemporaryViewQuery(viewName, query.getQuery()))
                    .executeUpdate();
            log.debug("executed create view statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            final List<String> columns = schemaService.inspectView(database, viewName)
                    .getColumns()
                    .stream()
                    .map(ViewColumnDto::getInternalName)
                    .toList();
            log.debug("executed inspect view columns statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            final Dataset<Row> dataset = getData(database, viewName, timestamp)
                    .selectExpr(columns.toArray(new String[0]));
            export = storageService.transformDataset(dataset);
            log.debug("executed extract statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.dropViewRawQuery(viewName))
                    .executeUpdate();
            log.debug("executed drop view statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        httpDataAccessCounter.increment();
        return export;
    }

    public QueryResultDto executeNonPersistent(PrivilegedDatabaseDto database, String statement,
                                               List<ColumnDto> columns) throws SQLException, TableMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(statement);
            final ResultSet resultSet = preparedStatement.executeQuery();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            httpDataAccessCounter.increment();
            return dataMapper.resultListToQueryResultDto(columns, resultSet);
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
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.countRawSelectQuery(statement, timestamp))
                    .executeQuery();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            httpDataAccessCounter.increment();
            return mariaDbMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new TableMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public QueryDto findById(PrivilegedDatabaseDto database, Long queryId) throws QueryNotFoundException, SQLException,
            RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(mariaDbMapper.queryStoreFindQueryRawQuery());
            preparedStatement.setLong(1, queryId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            if (!resultSet.next()) {
                throw new QueryNotFoundException("Failed to find query");
            }
            final QueryDto query = dataMapper.resultSetToQueryDto(resultSet);
            final List<IdentifierBriefDto> identifiers = metadataServiceGateway.getIdentifiers(database.getId(), queryId)
                    .stream()
                    .map(metadataMapper::identifierDtoToIdentifierBriefDto)
                    .toList();
            query.setIdentifiers(identifiers);
            query.setOwner(database.getOwner());
            query.setDatabaseId(database.getId());
            return query;
        } catch (SQLException e) {
            log.error("Failed to find query with id {}: {}", queryId, e.getMessage());
            throw new QueryNotFoundException("Failed to find query with id " + queryId + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public Long storeQuery(PrivilegedDatabaseDto database, String query, Instant timestamp, UUID userId) throws SQLException,
            QueryStoreInsertException {
        /* save */
        final Long queryId;
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* insert query into query store */
            final long start = System.currentTimeMillis();
            final CallableStatement callableStatement = connection.prepareCall(mariaDbMapper.queryStoreStoreQueryRawQuery());
            if (userId != null) {
                callableStatement.setString(1, String.valueOf(userId));
            } else {
                callableStatement.setNull(1, Types.VARCHAR);
            }
            callableStatement.setString(2, query);
            callableStatement.setTimestamp(3, Timestamp.from(timestamp));
            callableStatement.registerOutParameter(4, Types.BIGINT);
            callableStatement.executeUpdate();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
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
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(mariaDbMapper.queryStoreUpdateQueryRawQuery());
            preparedStatement.setBoolean(1, persist);
            preparedStatement.setLong(2, queryId);
            preparedStatement.executeUpdate();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
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
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreDeleteStaleQueriesRawQuery())
                    .executeUpdate();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
        } catch (SQLException e) {
            log.error("Failed to delete stale queries: {}", e.getMessage());
            throw new QueryStoreGCException("Failed to delete stale queries: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public Dataset<Row> getData(@NotNull PrivilegedDatabaseDto database, String viewName, Instant timestamp)
            throws ViewNotFoundException, QueryMalformedException {
        log.debug("get data from view: {}", viewName);
        try {
            final Properties properties = new Properties();
            properties.setProperty("user", database.getContainer().getUsername());
            properties.setProperty("password", database.getContainer().getPassword());
            return sparkSession.read()
                    .jdbc(getSparkUrl(database.getContainer(), database.getInternalName()),
                            mariaDbMapper.subsetToRawExportQuery(viewName, timestamp), properties);
        } catch (Exception e) {
            if (e instanceof ExtendedAnalysisException exception) {
                if (exception.getSimpleMessage().contains("TABLE_OR_VIEW_NOT_FOUND")) {
                    log.error("Failed to find temporary view {}: {}", viewName, exception.getSimpleMessage());
                    throw new ViewNotFoundException("Failed to find temporary view " + viewName + ": " + exception.getSimpleMessage()) /* remove throwable on purpose, clutters the output */;
                }
            }
            log.error("Failed to find get data from view: {}", e.getMessage());
            throw new QueryMalformedException("Failed to find get data from view: " + e.getMessage(), e);
        }
    }

}
