package at.tuwien.service.impl;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.entities.user.User;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
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
import java.util.List;

@Log4j2
@Service
public class StoreServiceImpl extends HibernateConnector implements StoreService {

    private final QueryMapper queryMapper;
    private final StoreMapper storeMapper;
    private final UserService userService;
    private final DatabaseService databaseService;

    @Autowired
    public StoreServiceImpl(QueryMapper queryMapper, StoreMapper storeMapper, UserService userService,
                            DatabaseService databaseService) {
        this.queryMapper = queryMapper;
        this.storeMapper = storeMapper;
        this.userService = userService;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Query> findAll(Long containerId, Long databaseId, Boolean persisted) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryStoreException, ContainerNotFoundException, DatabaseConnectionException,
            TableMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        log.trace("find all queries in database id {}", databaseId);
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        /* select all */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectAllQuery(connection, persisted);
            final ResultSet resultSet = preparedStatement.executeQuery();
            return storeMapper.resultSetToQueryList(resultSet);
        } catch (SQLException e) {
            log.error("Failed to find queries");
            log.debug("failed to find queries in container with id {} and database with id {}, reason: {}", containerId, databaseId, e.getMessage());
            throw new QueryStoreException("Query not found");
        } finally {
            dataSource.close();
        }

    }

    @Override
    @Transactional(readOnly = true)
    public Query findOne(Long containerId, Long databaseId, Long queryId) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, QueryNotFoundException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        /* use jpa to select one */
        final Query query;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectOneQuery(connection, containerId, databaseId, queryId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            query = storeMapper.resultSetToQuery(resultSet, true);
            return query;
        } catch (SQLException e) {
            log.error("Query not found with id {}", queryId);
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
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        log.trace("insert into database id {}, metadata {}", databaseId, metadata);
        /* user */
        final User creator = userService.findByUsername(principal.getName());
        /* save */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        final at.tuwien.querystore.Query query = at.tuwien.querystore.Query.builder()
                .cid(containerId)
                .dbid(databaseId)
                .query(metadata.getStatement())
                .type(storeMapper.queryTypeDtoToQueryType(type))
                .queryNormalized(metadata.getStatement())
                .queryHash(DigestUtils.sha256Hex(metadata.getStatement()))
                .resultNumber(storeMapper.queryResultDtoToLong(result))
                .resultHash(storeMapper.queryResultDtoToString(result))
                .execution(execution)
                .createdBy(creator.getId())
                .build();
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawInsertQuery(connection, query);
            final ResultSet resultSet = preparedStatement.executeQuery();
            query.setId(storeMapper.resultSetToId(resultSet));
            log.info("Inserted query with id {} into the query store of database with id {}", query.getId(), databaseId);
            log.debug("inserted query {} into the query store of database {}", query, database);
            return query;
        } catch (SQLException e) {
            log.error("Failed to execute query");
            log.debug("failed to execute query: {}", e.getMessage());
            throw new QueryStoreException("Failed to execute query", e);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional
    public Query persist(Long containerId, Long databaseId, Long queryId) throws DatabaseNotFoundException,
            ImageNotSupportedException, DatabaseConnectionException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* persist */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        final Query out;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawPersistQuery(connection, true, containerId, databaseId, queryId);
            preparedStatement.executeUpdate();
            final PreparedStatement preparedStatement1 = storeMapper.queryStoreRawSelectOneQuery(connection, containerId, databaseId, queryId);
            final ResultSet resultSet = preparedStatement1.executeQuery();
            out = storeMapper.resultSetToQuery(resultSet, true);
        } catch (SQLException e) {
            log.error("Failed to update query");
            log.debug("failed to update query, reason: {}", e.getMessage());
            throw new QueryStoreException("Failed to update query", e);
        } finally {
            dataSource.close();
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Query update(Long containerId, Long databaseId, QueryResultDto result, Long resultNumber, Query query)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException, DatabaseConnectionException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* save */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        query.setQueryHash(DigestUtils.sha256Hex(query.getQuery()));
        query.setResultNumber(resultNumber);
        query.setResultHash(storeMapper.queryResultDtoToString(result));
        final Query out;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawUpdateQuery(connection, query);
            preparedStatement.executeUpdate();
            final PreparedStatement preparedStatement1 = storeMapper.queryStoreRawSelectOneQuery(connection, containerId, databaseId, query.getId());
            final ResultSet resultSet = preparedStatement1.executeQuery();
            out = storeMapper.resultSetToQuery(resultSet, true);
        } catch (SQLException e) {
            log.debug("Failed to update query: {}", e.getMessage());
            throw new QueryStoreException("Failed to update query", e);
        } finally {
            dataSource.close();
        }
        return out;
    }


}
