package at.tuwien.service.impl;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.StoreMapper;
import at.tuwien.querystore.Query;
import at.tuwien.repository.mdb.IdentifierRepository;
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
import java.util.LinkedList;
import java.util.List;

@Log4j2
@Service
public class StoreServiceImpl extends HibernateConnector implements StoreService {

    private final StoreMapper storeMapper;
    private final UserService userService;
    private final DatabaseService databaseService;
    private final IdentifierRepository identifierRepository;

    @Autowired
    public StoreServiceImpl(StoreMapper storeMapper, UserService userService, DatabaseService databaseService,
                            IdentifierRepository identifierRepository) {
        this.storeMapper = storeMapper;
        this.userService = userService;
        this.databaseService = databaseService;
        this.identifierRepository = identifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Query> findAll(Long databaseId, Boolean persisted, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryStoreException,
            ContainerNotFoundException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        /* select all */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectAllQuery(connection, persisted);
            final ResultSet resultSet = preparedStatement.executeQuery();
            final List<Query> queries = new LinkedList<>();
            while (resultSet.next()) {
                queries.add(storeMapper.resultSetToQuery(resultSet));
            }
            return queries;
        } catch (SQLException e) {
            log.error("Failed to find queries in database with id {}: {}", databaseId, e.getMessage());
            throw new QueryStoreException("Failed to find queries in database with id " + database);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Query findOne(Long databaseId, Long queryId, Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException, QueryNotFoundException, QueryStoreException {
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* run query */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        /* use jpa to select one */
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectOneQuery(connection, queryId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                log.error("Query not found with id {} in database with id {}", queryId, databaseId);
                throw new QueryNotFoundException("Query not found with id " + queryId + "  in database with id " + databaseId);
            }
            return storeMapper.resultSetToQuery(resultSet);
        } catch (SQLException e) {
            log.error("Failed to retrieve first row for query with id {}: {}", queryId, e.getMessage());
            throw new QueryStoreException("Failed to retrieve first row for query with id " + queryId);
        } finally {
            dataSource.close();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Query insert(Long databaseId, ExecuteStatementDto metadata, Principal principal)
            throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            UserNotFoundException, DatabaseConnectionException, KeycloakRemoteException, AccessDeniedException,
            QueryNotFoundException {
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        final User user;
        if (principal == null) {
            user = userService.findByUsername("system");
        } else {
            user = userService.findByUsername(principal.getName());
        }
        /* save */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final CallableStatement callableStatement = storeMapper.queryStoreRawInsertQuery(connection, user, metadata);
            callableStatement.executeUpdate();
            final Long queryId = callableStatement.getLong(4);
            callableStatement.close();
            log.debug("inserted query with id {}", queryId);
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawSelectOneQuery(connection, queryId);
            final ResultSet resultSet = preparedStatement.executeQuery();
            if (!resultSet.next()) {
                log.error("Query not found with id {} in database with id {}", queryId, databaseId);
                throw new QueryNotFoundException("Query not found with id " + queryId + "  in database with id " + databaseId);
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
    public Query persist(Long databaseId, Long queryId, QueryPersistDto data) throws DatabaseNotFoundException,
            ImageNotSupportedException, QueryStoreException, IdentifierAlreadyPublishedException {
        /* check */
        if (!data.getPersist() && !identifierRepository.findByDatabaseIdAndQueryId(databaseId, queryId).isEmpty()) {
            log.error("Failed to de-persist query with id {} in database with id {}: identifier already attached", queryId, databaseId);
            throw new IdentifierAlreadyPublishedException("Failed to de-persist query with id " + queryId + " in database with id " + databaseId + ": identifier already attached");
        }
        /* find */
        final Database database = databaseService.find(databaseId);
        if (!database.getContainer().getImage().getName().equals("mariadb")) {
            log.error("Currently only MariaDB is supported");
            throw new ImageNotSupportedException("Currently only MariaDB is supported");
        }
        /* persist */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        final Query out;
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement = storeMapper.queryStoreRawPersistQuery(connection, data.getPersist(), queryId);
            preparedStatement.executeUpdate();
            final PreparedStatement preparedStatement1 = storeMapper.queryStoreRawSelectOneQuery(connection, queryId);
            final ResultSet resultSet = preparedStatement1.executeQuery();
            if (!resultSet.next()) {
                log.error("Failed to retrieve first row for query with id {} in database with id {}", queryId, databaseId);
                throw new QueryStoreException("Failed to retrieve first row for query with id " + queryId + "in database with id " + databaseId);
            }
            out = storeMapper.resultSetToQuery(resultSet);
        } catch (SQLException e) {
            log.error("Failed to update query: {}", e.getMessage());
            throw new QueryStoreException("Failed to update query", e);
        } finally {
            dataSource.close();
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteStaleQueries() throws ImageNotSupportedException, QueryStoreException {
        /* find */
        final List<Database> databases = databaseService.findAll();
        for (Database database : databases) {
            if (!database.getContainer().getImage().getName().equals("mariadb")) {
                log.error("Currently only MariaDB is supported");
                throw new ImageNotSupportedException("Currently only MariaDB is supported");
            }
            /* run query */
            final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                    database.getContainer(), database);
            /* delete stale queries older than 24hrs */
            try {
                final Connection connection = dataSource.getConnection();
                final PreparedStatement preparedStatement = storeMapper.queryStoreRawDeleteStaleQueries(connection);
                final int affected = preparedStatement.executeUpdate();
                log.debug("delete stale queries affected {} rows", affected);
            } catch (SQLException e) {
                log.error("Failed to delete stale queries in database with id {}: {}", database.getId(), e.getMessage());
                throw new QueryStoreException("Failed to delete stale queries in database with id " + database.getId(), e);
            } finally {
                dataSource.close();
            }
        }
    }


}
