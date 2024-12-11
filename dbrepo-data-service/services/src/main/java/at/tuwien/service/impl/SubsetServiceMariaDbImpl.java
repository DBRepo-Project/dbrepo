package at.tuwien.service.impl;

import at.tuwien.api.SortTypeDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.mapper.DataMapper;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.service.SubsetService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class SubsetServiceMariaDbImpl extends HibernateConnector implements SubsetService {

    private final DataMapper dataMapper;
    private final SparkSession sparkSession;
    private final MariaDbMapper mariaDbMapper;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public SubsetServiceMariaDbImpl(DataMapper dataMapper, SparkSession sparkSession, MariaDbMapper mariaDbMapper,
                                    MetadataServiceGateway metadataServiceGateway) {
        this.dataMapper = dataMapper;
        this.sparkSession = sparkSession;
        this.mariaDbMapper = mariaDbMapper;
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
    public Long create(PrivilegedDatabaseDto database, String statement, Instant timestamp, UUID userId)
            throws QueryStoreInsertException, SQLException {
        return storeQuery(database, statement, timestamp, userId);
    }

    @Override
    public Dataset<Row> reExecute(PrivilegedDatabaseDto database, QueryDto query, Long page, Long size,
                                  SortTypeDto sortDirection, String sortColumn)
            throws TableMalformedException, SQLException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.selectRawSelectQuery(query.getQuery(), query.getExecution(), page, size))
                    .executeQuery();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
            final List<String> columns = dataMapper.parseColumns(database.getId(), database.getTables(), query.getQuery())
                    .stream()
                    .map(ColumnDto::getInternalName)
                    .toList();
            return sparkSession.createDataFrame(mariaDbMapper.resultSetToList(resultSet, columns), Row.class);
        } catch (SQLException | JSQLParserException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new TableMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
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
    public Long executeCountNonPersistent(PrivilegedDatabaseDto database, String statement, Instant timestamp)
            throws SQLException, QueryMalformedException, TableMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.countRawSelectQuery(statement, timestamp))
                    .executeQuery();
            log.debug("executed statement in {} ms", System.currentTimeMillis() - start);
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
            query.setIdentifiers(metadataServiceGateway.getIdentifiers(database.getId(), queryId));
            query.setCreator(database.getOwner());
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

}
