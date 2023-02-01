package at.tuwien.service.impl;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
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
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryNotFoundException, QueryStoreException {
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
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectOneQuery(connection, queryId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                log.error("Query not found with id {}", queryId);
                throw new QueryNotFoundException("Query not found with id " + queryId);
            }
            return storeMapper.resultSetToQuery(resultSet);
        } catch (SQLException e) {
            log.error("Failed to retrieve first row for query with id {}, because {}", queryId, e.getMessage());
            throw new QueryStoreException("Failed to retrieve first row for query with id " + queryId);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Query insert(Long containerId, Long databaseId, ExecuteStatementDto metadata, Principal principal)
            throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, UserNotFoundException, DatabaseConnectionException, TableMalformedException {
        /* find */
        final Database database = databaseService.find(containerId, databaseId);
        if (!database.getContainer().getImage().getRepository().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        log.trace("insert into database id {}, metadata {}", databaseId, metadata);
        /* user */
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        final User creator;
        if (principal != null) {
            creator = userService.findByUsername(principal.getName());
        } else {
            creator = userService.findByUsername("system");
        }
        /* save */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(),
                database.getContainer(), database, root);
        try {
            final Connection connection = dataSource.getConnection();
            final CallableStatement callableStatement = storeMapper.queryStoreRawInsertQuery(connection, creator, metadata.getStatement());
            callableStatement.setString("_username", creator.getUsername());
            callableStatement.setString("query", metadata.getStatement());
            callableStatement.registerOutParameter("queryId", Types.BIGINT);
            callableStatement.executeUpdate();
            final Long queryId = callableStatement.getLong("queryId");
            callableStatement.close();
            log.debug("inserted query with id {}", queryId);
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectOneQuery(connection, queryId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                log.error("Failed to retrieve query with id {}", queryId);
                throw new QueryStoreException("Failed to retrieve query with id " + queryId);
            }
            final Query query = storeMapper.resultSetToQuery(resultSet);
            log.info("Found query with id {} into the query store of database with id {}", queryId, databaseId);
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
            if (!resultSet.next()) {
                log.error("Failed to retrieve first row for query with id {}", queryId);
                throw new QueryStoreException("Failed to retrieve first row for query with id " + queryId);
            }
            out = storeMapper.resultSetToQuery(resultSet);
        } catch (SQLException e) {
            log.error("Failed to update query, reason: {}", e.getMessage());
            throw new QueryStoreException("Failed to update query", e);
        } finally {
            dataSource.close();
        }
        return out;
    }

    protected List<Query> resultSetToQueryList(ResultSet resultSet) throws SQLException {
        final List<Query> queries = new LinkedList<>();
        while (resultSet.next()) {
            queries.add(storeMapper.resultSetToQuery(resultSet));
        }
        return queries;
    }


}
