package at.tuwien.service.impl;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.mapper.DataMapper;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.SubsetService;
import at.tuwien.service.TableService;
import at.tuwien.service.ViewService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class SubsetServiceMariaDbImpl extends DataConnector<DatabaseDto> implements SubsetService {

    private final DataMapper dataMapper;
    private final ViewService viewService;
    private final TableService tableService;
    private final MariaDbMapper mariaDbMapper;
    private final MetadataMapper metadataMapper;
    private final DatabaseService databaseService;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public SubsetServiceMariaDbImpl(DataMapper dataMapper, ViewService viewService, TableService tableService,
                                    MariaDbMapper mariaDbMapper, MetadataMapper metadataMapper,
                                    DatabaseService databaseService, MetadataServiceGateway metadataServiceGateway) {
        this.dataMapper = dataMapper;
        this.viewService = viewService;
        this.tableService = tableService;
        this.mariaDbMapper = mariaDbMapper;
        this.metadataMapper = metadataMapper;
        this.databaseService = databaseService;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @Override
    public Dataset<Row> getData(DatabaseDto database, QueryDto subset, Long page, Long size)
            throws ViewMalformedException, SQLException, QueryMalformedException, TableNotFoundException {
        final String viewName = metadataMapper.queryDtoToViewName(subset);
        if (!databaseService.existsView(database, viewName)) {
            log.warn("Missing internal view {} for subset with id {}: create it from subset query", viewName, subset.getId());
            databaseService.createView(database, ViewCreateDto.builder()
                    .isPublic(false)
                    .isSchemaPublic(false)
                    .name(viewName)
                    .query(subset.getQuery())
                    .build());
        } else {
            log.debug("internal view {}.{} for subset with id {} exists", database.getInternalName(), viewName, subset.getId());
        }
        return tableService.getData(database, viewName, subset.getExecution(), page, size, null, null);
    }

    @Override
    public Long create(DatabaseDto database, String statement, Instant timestamp, UUID userId)
            throws QueryStoreInsertException, SQLException {
        return storeQuery(database, statement, timestamp, userId);
    }

    @Override
    public Long reExecuteCount(DatabaseDto database, QueryDto query) throws TableMalformedException,
            SQLException, QueryMalformedException {
        return executeCountNonPersistent(database, query.getQuery(), query.getExecution());
    }

    @Override
    public List<QueryDto> findAll(DatabaseDto database, Boolean filterPersisted) throws SQLException,
            QueryNotFoundException, RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {
        final List<IdentifierBriefDto> identifiers = metadataServiceGateway.getIdentifiers(database.getId(), null);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final PreparedStatement statement = connection.prepareStatement(mariaDbMapper.filterToGetQueriesRawQuery(filterPersisted));
            if (filterPersisted != null) {
                statement.setBoolean(1, filterPersisted);
                log.trace("filter persisted only {}", filterPersisted);
            }
            final ResultSet resultSet = statement.executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            final List<QueryDto> queries = new LinkedList<>();
            while (resultSet.next()) {
                final QueryDto query = dataMapper.resultSetToQueryDto(resultSet);
                query.setIdentifiers(identifiers.stream()
                        .filter(i -> i.getType().equals(IdentifierTypeDto.SUBSET))
                        .filter(i -> i.getQueryId().equals(query.getId()))
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
    public Long executeCountNonPersistent(DatabaseDto database, String statement, Instant timestamp)
            throws SQLException, QueryMalformedException, TableMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.countRawSelectQuery(statement, timestamp))
                    .executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            return mariaDbMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new TableMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public QueryDto findById(DatabaseDto database, Long queryId) throws QueryNotFoundException, SQLException,
            RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(mariaDbMapper.queryStoreFindQueryRawQuery());
            preparedStatement.setLong(1, queryId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            if (!resultSet.next()) {
                throw new QueryNotFoundException("Failed to find query");
            }
            final QueryDto query = dataMapper.resultSetToQueryDto(resultSet);
            query.setIdentifiers(metadataServiceGateway.getIdentifiers(database.getId(), queryId));
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
    public Long storeQuery(DatabaseDto database, String query, Instant timestamp, UUID userId) throws SQLException,
            QueryStoreInsertException {
        /* save */
        final Long queryId;
        final ComboPooledDataSource dataSource = getDataSource(database);
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
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
    public void persist(DatabaseDto database, Long queryId, Boolean persist) throws SQLException,
            QueryStorePersistException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* update query */
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(mariaDbMapper.queryStoreUpdateQueryRawQuery());
            preparedStatement.setBoolean(1, persist);
            preparedStatement.setLong(2, queryId);
            preparedStatement.executeUpdate();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
        } catch (SQLException e) {
            log.error("Failed to (un-)persist query: {}", e.getMessage());
            throw new QueryStorePersistException("Failed to (un-)persist query", e);
        } finally {
            dataSource.close();
        }
        log.info("Performed (un-)persist for query with id {} in database with name {}", queryId, database.getInternalName());
    }

    @Override
    public void deleteStaleQueries(DatabaseDto database) throws SQLException, QueryStoreGCException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreDeleteStaleQueriesRawQuery())
                    .executeUpdate();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
        } catch (SQLException e) {
            log.error("Failed to delete stale queries: {}", e.getMessage());
            throw new QueryStoreGCException("Failed to delete stale queries: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

}
