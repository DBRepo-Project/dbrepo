package at.tuwien.service.impl;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.entities.user.User;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.mapper.StoreMapper;
import at.tuwien.querystore.Query;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.StoreService;
import at.tuwien.service.UserService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.sql.*;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@Log4j2
@Service
public class StoreServiceImpl extends HibernateConnector implements StoreService {

    private final StoreMapper storeMapper;
    private final UserService userService;
    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;

    @Autowired
    public StoreServiceImpl(StoreMapper storeMapper, UserService userService, DatabaseMapper databaseMapper,
                            DatabaseService databaseService) {
        this.storeMapper = storeMapper;
        this.userService = userService;
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Query> findAll(Long containerId, Long databaseId, Boolean persisted, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException,
            ContainerNotFoundException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        /* select all */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectAllQuery(connection, persisted);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return resultSetToQueryList(resultSet);
        } catch (SQLException e) {
            log.error("Failed to find queries in container with id {} and database with id {}, reason: {}", containerId, databaseId, e.getMessage());
            throw new QueryStoreException("Failed to find queries: " + e.getMessage());
        } finally {
            dataSource.close();
        }

    }

    @Override
    @Transactional(readOnly = true)
    public Query findOne(Long containerId, Long databaseId, Long queryId, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryNotFoundException, QueryStoreException,
            UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        /* use jpa to select one */
        final Query query;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectOneQuery(connection, queryId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return resultSetToQuery(resultSet, false);
        } catch (SQLException e) {
            log.error("Query not found with id {}, because {}", queryId, e.getMessage());
            throw new QueryNotFoundException("Query not found");
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Query insert(Long containerId, Long databaseId, QueryResultDto result, ExecuteStatementDto metadata,
                        QueryTypeDto type, Principal principal, Instant execution)
            throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, UserNotFoundException, DatabaseConnectionException, TableMalformedException {
        /* check */
        if (type.equals(QueryTypeDto.VIEW)) {
            /* view executions are not stored in the query store */
            return Query.builder()
                    .query(metadata.getStatement())
                    .queryNormalized(metadata.getStatement())
                    .queryHash(DigestUtils.sha256Hex(metadata.getStatement()))
                    .resultNumber(null)
                    .resultHash(null)
                    .createdBy(null)
                    .build();
        }
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        log.debug("insert into database id {}, metadata {}", databaseId, metadata);
        /* user */
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        final User creator = userService.findByUsername(principal.getName());
        /* save */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        final Query query = at.tuwien.querystore.Query.builder()
                .query(metadata.getStatement())
                .queryNormalized(metadata.getStatement())
                .queryHash(DigestUtils.sha256Hex(metadata.getStatement()))
                .resultNumber(storeMapper.queryResultDtoToLong(result))
                .resultHash(storeMapper.queryResultDtoToString(result))
                .createdBy(creator.getId())
                .build();
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawInsertQuery(connection, creator, query);
            final ResultSet resultSet = preparedStatement.executeQuery();
            query.setId(storeMapper.resultSetToId(resultSet));
            log.info("Inserted query with id {} into the query store of database with id {}", query.getId(), databaseId);
            log.debug("inserted query {} into the query store of database {}", query, database);
            return query;
        } catch (SQLException e) {
            log.error("Failed to execute query: {}", e.getMessage());
            throw new QueryStoreException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public Query persist(Long containerId, Long databaseId, Long queryId, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* persist */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        final Query out;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawPersistQuery(connection, true, queryId);
            preparedStatement.executeUpdate();
            final PreparedStatement preparedStatement1 = storeMapper.queryStoreRawSelectOneQuery(connection, queryId);
            final ResultSet resultSet = preparedStatement1.executeQuery();
            out = resultSetToQuery(resultSet, false);
        } catch (SQLException e) {
            log.error("Failed to update query, reason: {}", e.getMessage());
            throw new QueryStoreException("Failed to update query", e);
        } finally {
            dataSource.close();
        }
        return out;
    }

    protected List<Query> resultSetToQueryList(ResultSet resultSet) throws SQLException, UserNotFoundException {
        final List<Query> queries = new LinkedList<>();
        while (resultSet.next()) {
            queries.add(resultSetToQuery(resultSet, false));
        }
        return queries;
    }

    /**
     * Maps a result set row to an entity with columns `id`, `created`, `created_by`, `last_modified`, `query`,
     * `query_hash`, `result_hash`, `result_number`, `is_persisted`
     *
     * @param data The result set row.
     * @return The query.
     * @throws SQLException The mapping does not exist
     */
    protected Query resultSetToQuery(ResultSet data, Boolean next) throws SQLException, UserNotFoundException {
        if (next && !data.next()) {
            throw new SQLException("Tuple does not exist");
        }
        final Query dto = Query.builder()
                .id(data.getLong(1))
                .created(data.getTimestamp(2)
                        .toInstant())
                .createdBy(userService.findByUsername(data.getString(3)).getId())
                .lastModified(data.getTimestamp(4) != null ? data.getTimestamp(7)
                        .toInstant() : null)
                .query(data.getString(5))
                .queryHash(data.getString(6))
                .resultHash(data.getString(7) != null ? data.getString(10) : null)
                .resultNumber(data.getLong(8))
                .isPersisted(data.getBoolean(9))
                .build();
        log.trace("mapped result set {} to query {}", data, dto);
        return dto;
    }


}
