package at.tuwien.service.impl;

import at.tuwien.utils.FileUtil;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryStoreService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Log4j2
@Service
public class QueryStoreServiceImpl extends HibernateConnector implements QueryStoreService {

    private final DatabaseMapper databaseMapper;
    private final DatabaseService databaseService;

    @Autowired
    public QueryStoreServiceImpl(DatabaseMapper databaseMapper, DatabaseService databaseService) {
        this.databaseMapper = databaseMapper;
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(rollbackFor = DatabaseMalformedException.class)
    public void create(Long containerId, Long databaseId, Principal principal) throws DatabaseNotFoundException,
            DatabaseConnectionException, DatabaseMalformedException, UserNotFoundException, QueryStoreException {
        final Database database = databaseService.findById(containerId, databaseId);
        final User root = databaseMapper.containerToPrivilegedUser(database.getContainer());
        /* create */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database, root);
        try {
            final Connection connection = dataSource.getConnection();
            for (String query : FileUtil.loadResource("/init/querystore.sql")) {
                executeQuery(connection, query);
            }
        } catch (SQLException e) {
            log.error("Failed to create query store {}, reason: {}", database, e.getMessage());
            throw new DatabaseMalformedException("Failed to create database", e);
        } catch (IOException e) {
            log.error("Failed to load query store init script, reason: {}", e.getMessage());
            throw new QueryStoreException("Failed to load query store init script");
        } finally {
            dataSource.close();
        }
        log.info("Created query store in database with id {}", databaseId);
        log.trace("created query store in database {}", database);
    }

    public void executeQuery(Connection connection, String statement) throws SQLException {
        log.debug("execute query, statement={}", statement);
        final PreparedStatement pstmt = connection.prepareStatement(statement);
        pstmt.executeUpdate();
    }

}
