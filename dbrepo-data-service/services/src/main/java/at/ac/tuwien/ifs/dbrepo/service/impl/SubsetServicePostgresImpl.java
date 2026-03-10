package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.api.SubsetMetadata;
import at.ac.tuwien.ifs.dbrepo.cache.SubsetCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.SubsetType;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.PostgresMapper;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SubsetServicePostgresImpl extends DataConnector implements SubsetService {

    private final DSLContext context;
    private final DataMapper dataMapper;
    private final PostgresMapper mariaDbMapper;
    private final SubsetCacheRepository subsetRepository;
    private final MetadataServiceGateway metadataServiceGateway;

    @Autowired
    public SubsetServicePostgresImpl(DSLContext context, DataMapper dataMapper, PostgresMapper mariaDbMapper,
                                     SubsetCacheRepository subsetRepository,
                                     MetadataServiceGateway metadataServiceGateway) {
        this.context = context;
        this.dataMapper = dataMapper;
        this.mariaDbMapper = mariaDbMapper;
        this.subsetRepository = subsetRepository;
        this.metadataServiceGateway = metadataServiceGateway;
    }

    @Override
    @Timed(value = "dbrepo_data_create_subset", description = "Time spent creating a subset", histogram = true)
    public UUID create(Database database, SubsetDto subset, Instant timestamp, String username)
            throws QueryStoreInsertException, SQLException, QueryMalformedException, TableNotFoundException,
            ImageNotFoundException, ViewNotFoundException, ColumnNotFoundException {
        final String query = mariaDbMapper.subsetDtoToNormalizedQuery(context, database, subset);
        final String normalizedQuery = mariaDbMapper.subsetDtoToNormalizedTimestampedQuery(context, database, subset, timestamp);
        return storeQuery(database, query, normalizedQuery, timestamp, username);
    }

    @Timed(value = "dbrepo_data_hash_subset_data", description = "Time spent hashing subset data", histogram = true)
    @Override
    public SubsetMetadata getMetadata(Database database, String statement) throws SQLException, QueryExecutionException,
            QueryMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final CallableStatement callableStatement = connection.prepareCall(mariaDbMapper.queryStoreHashQueryRawQuery());
            callableStatement.setString(1, statement);
            callableStatement.registerOutParameter(2, Types.VARCHAR);
            log.atDebug()
                    .setMessage("hash subset in database " + database.getInternalName() + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "hash_subset")
                    .addKeyValue("query", statement)
                    .log();
            callableStatement.executeUpdate();
            final String resultHash = callableStatement.getString(2);
            final ResultSet resultSet = connection.prepareStatement(mariaDbMapper.countRawSelectQuery(statement))
                    .executeQuery();
            log.atDebug()
                    .setMessage("count subset in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "count_query")
                    .addKeyValue("query", statement)
                    .log();
            final Long resultCount = mariaDbMapper.resultSetToNumber(resultSet);
            log.info("Computed result set metadata: count {} and hash: {}", resultCount, resultHash);
            return SubsetMetadata.builder()
                    .resultCount(resultCount)
                    .resultHash(resultHash)
                    .build();
        } catch (SQLException e) {
            log.error("Failed to map object: {}", e.getMessage());
            throw new QueryExecutionException("Failed to map object: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Timed(value = "dbrepo_data_find_subsets", description = "Time spent finding all subsets", histogram = true)
    public List<QueryDto> findAll(Database database, Boolean filterPersisted) throws SQLException,
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
                    .setMessage("list subsets in database " + database.getInternalName() + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "list_queries")
                    .log();
            final List<QueryDto> queries = new LinkedList<>();
            while (resultSet.next()) {
                final QueryDto subset = dataMapper.resultSetToQueryDto(resultSet);
                subset.setIdentifiers(identifiers.stream()
                        .filter(i -> i.getType().equals(IdentifierTypeDto.SUBSET))
                        .filter(i -> i.getQueryId().equals(subset.getId()))
                        .toList());
                if (subset.getOwner().getUsername() != null) {
                    subset.setOwner(subset.getOwner());
                } else {
                    subset.getOwner().setUsername("anonymous");
                }
                subset.setType(QueryTypeDto.QUERY);
                subset.setDatabaseId(database.getId());
                queries.add(subset);
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
    @Timed(value = "dbrepo_data_find_subset", description = "Time spent finding a specific subset", histogram = true)
    public Subset findById(Database database, UUID queryId) throws QueryNotFoundException, SQLException {
        final Optional<Subset> optional = subsetRepository.findById(queryId);
        if (optional.isPresent()) {
            log.trace("cache hit for subset: {}", queryId);
            return optional.get();
        }
        log.trace("cache miss for subset: {}", queryId);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(mariaDbMapper.queryStoreFindQueryRawQuery());
            preparedStatement.setString(1, String.valueOf(queryId));
            final ResultSet resultSet = preparedStatement.executeQuery();
            log.atDebug()
                    .setMessage("find query in query store of database " + database.getInternalName() + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "find_query")
                    .log();
            if (!resultSet.next()) {
                throw new QueryNotFoundException("Failed to find query");
            }
            final Subset subset = dataMapper.resultSetToSubset(resultSet);
            subset.setType(SubsetType.QUERY);
            subset.setDatabaseId(database.getId());
            return subsetRepository.save(subset);
        } catch (SQLException e) {
            log.error("Failed to find query with id {}: {}", queryId, e.getMessage());
            throw new QueryNotFoundException("Failed to find query with id " + queryId + ": " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Timed(value = "dbrepo_data_store_subset_query", description = "Time spent storing a subset query in the query store", histogram = true)
    public UUID storeQuery(Database database, String query, String normalizedQuery, Instant timestamp, String username)
            throws SQLException, QueryStoreInsertException {
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
            callableStatement.setString(3, normalizedQuery);
            callableStatement.setTimestamp(4, Timestamp.from(Instant.now()));
            callableStatement.registerOutParameter(5, Types.VARCHAR);
            callableStatement.executeUpdate();
            log.atDebug()
                    .setMessage("store query in query store of database " + database.getInternalName() + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "store_query")
                    .addKeyValue("query", query)
                    .log();
            queryId = UUID.fromString(callableStatement.getString(5));
            callableStatement.close();
            log.info("Stored query with id {} in database with name {}", queryId, database.getInternalName());
            return queryId;
        } catch (SQLException e) {
            log.error("Failed to store query: {}", e.getMessage());
            throw new QueryStoreInsertException("Failed to store query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Timed(value = "dbrepo_data_persist_subset", description = "Time spent persisting a query in the query store", histogram = true)
    public void persist(Database database, UUID subsetId, Boolean persist) throws SQLException,
            QueryStorePersistException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* update query */
            final long start = System.currentTimeMillis();
            final PreparedStatement preparedStatement = connection.prepareStatement(mariaDbMapper.queryStoreUpdateQueryRawQuery());
            preparedStatement.setBoolean(1, persist);
            preparedStatement.setString(2, String.valueOf(subsetId));
            preparedStatement.executeUpdate();
            log.atDebug()
                    .setMessage("persist query in query store of database " + database.getInternalName() + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "persist_query")
                    .addKeyValue("query_id", subsetId)
                    .log();
        } catch (SQLException e) {
            log.error("Failed to (un-)persist query: {}", e.getMessage());
            throw new QueryStorePersistException("Failed to (un-)persist query", e);
        } finally {
            dataSource.close();
        }
        subsetRepository.deleteById(subsetId);
        log.info("Performed (un-)persist for query with id {} in database with name {}", subsetId, database.getInternalName());
    }

}
