package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.core.entity.replication.TupleReplicationTimestamp;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationTimestampService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.ExtendedAnalysisException;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SubsetServiceMariaDbImpl extends DataConnector implements SubsetService {

    private final DSLContext context;
    private final DataMapper dataMapper;
    private final SparkSession sparkSession;
    private final MariaDbMapper mariaDbMapper;
    private final MetadataServiceGateway metadataServiceGateway;
    private final ReplicationTimestampService replicationTimestampService;

    @Autowired
    public SubsetServiceMariaDbImpl(DSLContext context, DataMapper dataMapper, MariaDbMapper mariaDbMapper,
                                    SparkSession sparkSession, MetadataServiceGateway metadataServiceGateway,
                                    ReplicationTimestampService replicationTimestampService) {
        this.context = context;
        this.dataMapper = dataMapper;
        this.sparkSession = sparkSession;
        this.mariaDbMapper = mariaDbMapper;
        this.metadataServiceGateway = metadataServiceGateway;
        this.replicationTimestampService = replicationTimestampService;
    }

    @Override
    @Timed(value = "dbrepo_data_get_subset_data", description = "Time spent getting data from subset", histogram = true)
    public Dataset<Row> getData(DatabaseDto database, String query) throws QueryMalformedException, TableNotFoundException {
        try {
            final long start = System.currentTimeMillis();
            final Dataset<Row> dataset = sparkSession.read()
                    .format("jdbc")
                    .option("user", database.getContainer().getUsername())
                    .option("password", database.getContainer().getPassword())
                    .option("url", getSparkUrl(database))
                    .option("query", query)
                    .load();
            log.atDebug()
                    .setMessage("get data from url: " + getSparkUrl(database))
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "jdbc_get_data")
                    .log();
            return dataset;
        } catch (Exception e) {
            if (e instanceof ExtendedAnalysisException && e.getMessage().contains("TABLE_OR_VIEW_NOT_FOUND")
                    || e instanceof SQLSyntaxErrorException && e.getMessage().contains("doesn't exist")) {
                log.atError()
                        .setMessage("Failed to find named reference")
                        .setCause(e)
                        .log();
                throw new TableNotFoundException("Failed to find named reference: " + e.getMessage()) /* remove throwable on purpose, clutters the output */;
            }
            log.error("Malformed query: {}", e.getMessage());
            throw new QueryMalformedException("Malformed query: " + e.getMessage(), e);
        }
    }

    @Override
    @Timed(value = "dbrepo_data_create_subset", description = "Time spent creating a subset", histogram = true)
    public UUID create(DatabaseDto database, SubsetDto subset, Instant timestamp, String username, String creationLocation)
            throws QueryStoreInsertException, SQLException, QueryMalformedException, TableNotFoundException,
            ImageNotFoundException, ViewMalformedException, ViewNotFoundException, ColumnNotFoundException {
        final String query = mariaDbMapper.subsetDtoToRawQuery(context, database, subset);
        return storeQuery(database, query, timestamp, username, creationLocation);
    }

    @Override
    @Timed(value = "dbrepo_data_count_subset_data", description = "Time spent counting subset data", histogram = true)
    public Long reExecuteCount(DatabaseDto database, QueryDto query) throws SQLException, QueryMalformedException {
        return executeCountNonPersistent(database, query.getQuery(), query.getExecution());
    }

    @Override
    @Timed(value = "dbrepo_data_find_subsets", description = "Time spent finding all subsets", histogram = true)
    public List<QueryDto> findAll(DatabaseDto database, Boolean filterPersisted) throws SQLException,
            QueryNotFoundException, RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException, UserNotFoundException {
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
            log.atDebug()
                    .setMessage("list subsets in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "list_queries")
                    .log();
            final List<QueryDto> queries = new LinkedList<>();
            while (resultSet.next()) {
                final QueryDto query = dataMapper.resultSetToQueryDto(resultSet);
                query.setIdentifiers(identifiers.stream()
                        .filter(i -> i.getType().equals(IdentifierTypeDto.SUBSET))
                        .filter(i -> i.getQueryId().equals(query.getId()))
                        .toList());
                if (query.getOwner().getUsername() != null) {
                    query.setOwner(dataMapper.userDtoToUserBriefDto(metadataServiceGateway.getUserByUsername(query.getOwner()
                            .getUsername())));
                } else {
                    query.getOwner().setUsername("anonymous");
                }
                query.setType(QueryTypeDto.QUERY);
                query.setDatabaseId(database.getId());

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
    @Timed(value = "dbrepo_data_execute_subset", description = "Time spent executing a specific subset", histogram = true)
    public Long executeCountNonPersistent(DatabaseDto database, String statement, Instant timestamp)
            throws SQLException, QueryMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.countRawSelectQuery(statement, timestamp))
                    .executeQuery();
            log.atDebug()
                    .setMessage("count subset in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "count_query")
                    .addKeyValue("query", statement)
                    .addKeyValue("timestamp", timestamp)
                    .log();
            return mariaDbMapper.resultSetToNumber(resultSet);
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new QueryMalformedException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Timed(value = "dbrepo_data_find_subset", description = "Time spent finding a specific subset", histogram = true)
    public QueryDto findById(DatabaseDto database, UUID queryId) throws QueryNotFoundException, SQLException,
            UserNotFoundException, RemoteUnavailableException, MetadataServiceException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(mariaDbMapper.queryStoreFindQueryRawQuery());
            preparedStatement.setString(1, String.valueOf(queryId));
            final ResultSet resultSet = preparedStatement.executeQuery();
            log.atDebug()
                    .setMessage("find query in query store of database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "find_query")
                    .log();
            if (!resultSet.next()) {
                throw new QueryNotFoundException("Failed to find query");
            }
            final QueryDto query = dataMapper.resultSetToQueryDto(resultSet);
            query.setOwner(dataMapper.userDtoToUserBriefDto(metadataServiceGateway.getUserByUsername(query.getOwner()
                    .getUsername())));
            query.setType(QueryTypeDto.QUERY);
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
    @Timed(value = "dbrepo_data_store_subset_query", description = "Time spent storing a subset query in the query store", histogram = true)
    public UUID storeQuery(DatabaseDto database, String query, Instant timestamp, String username, String creationLocation) throws SQLException,
            QueryStoreInsertException, QueryMalformedException {
        /* save */
        final UUID queryId;
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* insert query into query store */
            final long start = System.currentTimeMillis();
            final CallableStatement callableStatement = connection.prepareCall(mariaDbMapper.queryStoreStoreQueryRawQuery());
            if (username != null) {
                callableStatement.setString(1, username);
            } else {
                callableStatement.setNull(1, Types.VARCHAR);
            }
            callableStatement.setString(2, query);
            callableStatement.setString(3, mariaDbMapper.normalizeQuery(query, timestamp));
            callableStatement.setTimestamp(4, Timestamp.from(timestamp));
            callableStatement.setString(5, creationLocation);
            callableStatement.registerOutParameter(6, Types.VARCHAR);
            callableStatement.executeUpdate();
            log.atDebug()
                    .setMessage("store query in query store of database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "store_query")
                    .addKeyValue("query", query)
                    .log();
            queryId = UUID.fromString(callableStatement.getString(6));
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
    @Timed(value = "dbrepo_data_persist_subset", description = "Time spent persisting a query in the query store", histogram = true)
    public void persist(DatabaseDto database, UUID queryId, Boolean persist) throws SQLException,
            QueryStorePersistException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* update query */
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(mariaDbMapper.queryStoreUpdateQueryRawQuery());
            preparedStatement.setBoolean(1, persist);
            preparedStatement.setString(2, String.valueOf(queryId));
            preparedStatement.executeUpdate();
            log.atDebug()
                    .setMessage("persist query in query store of database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "persist_query")
                    .addKeyValue("query_id", queryId)
                    .log();
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
            log.atDebug()
                    .setMessage("delete stale queries in query store of database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "delete_stale_queries")
                    .log();
        } catch (SQLException e) {
            log.error("Failed to delete stale queries: {}", e.getMessage());
            throw new QueryStoreGCException("Failed to delete stale queries: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Timed(value = "dbrepo_data_replicate_query", description = "Time spent replicating a query from another instance", histogram = true)
    public UUID replicateQuery(DatabaseDto database, QueryDto queryDto) throws SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* directly insert the replicated query into qs_queries */
            final long start = System.currentTimeMillis();
            

            
            // Insert the replicated query directly
            final PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO qs_queries (id, created_by, query, query_normalized, is_persisted, " +
                "query_hash, result_hash, result_number, created, executed, creation_location) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            // Set all the values from the replicated QueryDto
            insertStatement.setString(1, queryDto.getId().toString());  // Use original ID
            insertStatement.setString(2, queryDto.getOwner().getUsername());
            insertStatement.setString(3, queryDto.getQuery());
            insertStatement.setString(4, queryDto.getQueryNormalized());
            insertStatement.setBoolean(5, queryDto.getIsPersisted());
            insertStatement.setString(6, queryDto.getQueryHash());
            insertStatement.setString(7, queryDto.getResultHash());
            insertStatement.setLong(8, queryDto.getResultNumber());
            insertStatement.setTimestamp(9, Timestamp.from(queryDto.getCreated()));
            insertStatement.setTimestamp(10, Timestamp.from(queryDto.getExecution()));
            insertStatement.setString(11, queryDto.getCreationLocation());  // Include creation location
            
            log.trace("Executing replication insert for query: {}", queryDto.getId());
            insertStatement.executeUpdate();
            
            log.atDebug()
                    .setMessage("replicate query in query store of database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "replicate_query")
                    .addKeyValue("query_id", queryDto.getId())
                    .addKeyValue("query_hash", queryDto.getQueryHash())
                    .log();
            
            connection.commit();
            log.info("Successfully replicated query with ID: {} in database: {}", 
                    queryDto.getId(), database.getInternalName());
            
            return queryDto.getId();  // Return the original ID since we preserved it
            
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to replicate query: {}", e.getMessage());
            throw new SQLException("Failed to replicate query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    public String checkIfQueryNeedsModification(DatabaseDto database, QueryDto queryDto, String currentBaseUrl) {
        String creationLocation = queryDto.getCreationLocation();
        
        // Query needs modification if:
        // 1. creationLocation is not null AND
        // 2. creationLocation is different from current baseUrl
        if (creationLocation != null && !creationLocation.equals(currentBaseUrl)) {
            log.info("Query needs to be modified: creationLocation={}, current baseUrl={}", 
                    creationLocation, currentBaseUrl);
            
            try {
                // Modify the query to use replication timestamps
                String modifiedQuery = modifyQueryWithReplicationTimestamps(database, queryDto.getQueryNormalized(), creationLocation, queryDto.getExecution());
                log.info("Query successfully modified with replication timestamps");
                log.info(modifiedQuery);
                return modifiedQuery;
            } catch (Exception e) {
                log.error("Failed to modify query with replication timestamps: {}", e.getMessage(), e);
                log.info("Falling back to original query due to modification failure");
                return queryDto.getQueryNormalized();
            }
        } else {
            log.debug("Query does not need modification: creationLocation={}, current baseUrl={}", 
                    creationLocation, currentBaseUrl);
            return queryDto.getQueryNormalized();
        }
    }
    
    /**
     * Modifies a SQL query to JOIN with tuple_replication_timestamps table and filter on the joined timestamps.
     * This replaces filtering on MariaDB's internal row_start/row_end with filtering on replication timestamps.
     */
    private String modifyQueryWithReplicationTimestamps(DatabaseDto database, String originalQuery, String creationLocation, Instant queryExecutionTimestamp) throws Exception {
        // Extract table names from the SQL query
        Set<String> tableNames = extractTableNames(originalQuery);
        log.debug("Extracted table names from query: {}", tableNames);
        
        // First, let's see what timestamps are available in this database
        List<TupleReplicationTimestamp> allTimestamps = replicationTimestampService
            .findBySiteUrl(database, creationLocation);
        log.info("Found {} total timestamps for site '{}' in database '{}'", 
                allTimestamps.size(), creationLocation, database.getInternalName());
        
        if (allTimestamps.isEmpty()) {
            log.warn("No timestamps found at all for site '{}' in database '{}'", creationLocation, database.getInternalName());
            log.warn("Query will NOT be modified - returning original query");
            return originalQuery;
        }
        
        // Log all available timestamps for debugging
        for (TupleReplicationTimestamp ts : allTimestamps) {
            log.debug("Available timestamp: site_url={}, replication_id={}, database_id={}, table_id={}, row_start={}, row_end={}", 
                    ts.getSiteUrl(), ts.getReplicationId(), ts.getDatabaseId(), ts.getTableId(), ts.getRowStart(), ts.getRowEnd());
        }
        
        String modifiedQuery = originalQuery;
        
        for (String tableName : tableNames) {
            try {
                // Use the already fetched timestamps instead of calling the service again
                log.debug("Processing table: {}", tableName);
                
                // Filter timestamps for this specific table (by name pattern matching)
                List<TupleReplicationTimestamp> tableTimestamps = allTimestamps.stream()
                    .filter(t -> t.getTableId() != null) // Ensure table ID is not null
                    .toList();
                
                log.debug("Found {} timestamps with table_id for table: {}", tableTimestamps.size(), tableName);
                
                if (!tableTimestamps.isEmpty()) {
                    // Log the timestamps we found
                    for (TupleReplicationTimestamp ts : tableTimestamps) {
                        log.info("Timestamp for table {}: site_url={}, replication_id={}, table_id={}, row_start={}, row_end={}",
                                tableName, ts.getSiteUrl(), ts.getReplicationId(), ts.getTableId(), ts.getRowStart(), ts.getRowEnd());
                    }
                    
                    // Use full table name as alias for JOIN references only
                    String tableAlias = tableName;
                    
                    // DON'T modify the FROM clause - keep original table name as-is
                    // The alias will only be used for the tuple_replication_timestamps table
                    
                    // Create the JOIN clause with tuple_replication_timestamps
                    // JOIN condition: replication_id from data table = replication_id from tuple_replication_timestamps
                    
                    // Format the query execution timestamp for the filter
                    String executionTimestampStr = queryExecutionTimestamp
                        .atZone(ZoneId.of("UTC"))
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    
                    String joinClause = String.format(
                        "JOIN tuple_replication_timestamps trt_%s ON trt_%s.site_url = '%s' " +
                        "AND trt_%s.replication_id = %s.replication_id " +
                        "AND trt_%s.row_start <= '%s' " +
                        "AND trt_%s.row_end > '%s'",
                        tableAlias, tableAlias, creationLocation, tableAlias, tableName, tableAlias, executionTimestampStr, tableAlias, executionTimestampStr
                    );
                    
                    log.debug("Generated JOIN clause: {}", joinClause);
                    
                    // Insert the JOIN clause after the table reference
                    modifiedQuery = insertJoinClause(modifiedQuery, tableName, tableAlias, joinClause);
                    
                    log.debug("Added JOIN clause for table '{}' with alias '{}'", tableName, tableAlias);
                } else {
                    log.warn("No replication timestamps found for table '{}' and creation location '{}'", 
                            tableName, creationLocation);
                    log.warn("This means the query will NOT be modified and will use original MariaDB timestamps");
                    log.warn("Available timestamps for site '{}':", creationLocation);
                    for (TupleReplicationTimestamp ts : allTimestamps) {
                        log.warn("  - site_url={}, replication_id={}, table_id={}, row_start={}, row_end={}", 
                                ts.getSiteUrl(), ts.getReplicationId(), ts.getTableId(), ts.getRowStart(), ts.getRowEnd());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to look up replication timestamp for table '{}': {}", tableName, e.getMessage());
                // Continue with other tables
            }
        }
        
        log.info("Original query: {}", originalQuery);
        log.info("Modified query: {}", modifiedQuery);
        
        return modifiedQuery;
    }
    
    /**
     * Inserts a JOIN clause after the table reference
     */
    private String insertJoinClause(String query, String tableName, String tableAlias, String joinClause) {
        log.debug("Attempting to insert JOIN clause for table '{}' with alias '{}'", tableName, tableAlias);
        log.debug("Query before insertion: {}", query);
        
        // Find the position after the table reference
        // Handle both cases: `tableName alias` and tableName alias
        Pattern tablePattern = Pattern.compile("(?i)(?:FROM|JOIN)\\s+`?([a-zA-Z_][a-zA-Z0-9_]*)\\s+`?([a-zA-Z_][a-zA-Z0-9_]*)`?");
        Matcher tableMatcher = tablePattern.matcher(query);
        
        if (tableMatcher.find()) {
            int tableEnd = tableMatcher.end();
            log.debug("Found table reference at position: {}", tableEnd);
            
            // Find the next clause (WHERE, GROUP BY, etc.)
            Pattern nextClausePattern = Pattern.compile("(?i)\\b(WHERE|GROUP BY|HAVING|ORDER BY|LIMIT|UNION|INTERSECT|EXCEPT)\\b");
            Matcher nextClauseMatcher = nextClausePattern.matcher(query);
            
            int nextClausePos = -1;
            if (nextClauseMatcher.find(tableEnd)) {
                nextClausePos = nextClauseMatcher.start();
                log.debug("Found next clause '{}' at position: {}", nextClauseMatcher.group(1), nextClausePos);
            }
            
            if (nextClausePos != -1) {
                // Insert JOIN before the next clause
                String beforeNextClause = query.substring(0, nextClausePos);
                String afterNextClause = query.substring(nextClausePos);
                String result = beforeNextClause + " " + joinClause + " " + afterNextClause;
                log.debug("Inserted JOIN before next clause. Result: {}", result);
                return result;
            } else {
                // No next clause, add JOIN at the end
                String result = query + " " + joinClause;
                log.debug("No next clause found, added JOIN at end. Result: {}", result);
                return result;
            }
        } else {
            log.warn("Could not find table reference pattern for table '{}' with alias '{}'", tableName, tableAlias);
            log.warn("Query: {}", query);
            log.warn("Table pattern: {}", tablePattern.pattern());
        }
        
        return query;
    }
    
    /**
     * Extracts table names from a SQL query using regex patterns.
     * This is a simplified approach - you might want a more robust SQL parser.
     */
    private Set<String> extractTableNames(String sqlQuery) {
        Set<String> tableNames = new HashSet<>();
        
        // Simple regex to find table names after FROM and JOIN
        // This handles basic cases but might need enhancement for complex queries
        Pattern pattern = Pattern.compile(
            "(?i)(?:FROM|JOIN)\\s+`?([a-zA-Z_][a-zA-Z0-9_]*)`?");
        Matcher matcher = pattern.matcher(sqlQuery);
        
        while (matcher.find()) {
            String tableName = matcher.group(1);
            tableNames.add(tableName);
            log.debug("Extracted table name: '{}' from query", tableName);
        }
        
        log.info("Extracted {} table names: {}", tableNames.size(), tableNames);
        return tableNames;
    }

}
