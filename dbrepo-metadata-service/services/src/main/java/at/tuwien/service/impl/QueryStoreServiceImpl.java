package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryStoreService;
import at.tuwien.utils.FileUtil;
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

    private final DatabaseService databaseService;

    @Autowired
    public QueryStoreServiceImpl(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    @Transactional(rollbackFor = DatabaseMalformedException.class)
    public void create(Long databaseId, Principal principal) throws DatabaseNotFoundException,
            DatabaseMalformedException, UserNotFoundException, QueryStoreException {
        final Database database = databaseService.findById(databaseId);
        /* create */
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            for (String query : FileUtil.loadResource("/init/querystore.sql")) {
                executeQuery(connection, query);
            }
        } catch (SQLException e) {
            log.error("Failed to create query store in database with id {}: {}", databaseId, e.getMessage());
            throw new DatabaseMalformedException("Failed to create query store in database with id " + databaseId, e);
        } catch (IOException e) {
            log.error("Failed to load query store init script: {}", e.getMessage());
            throw new QueryStoreException("Failed to load query store init script", e);
        } finally {
            dataSource.close();
        }
        log.info("Created query store in database with id {}", databaseId);
    }

    public void executeQuery(Connection connection, String statement, String... data) throws SQLException {
        log.debug("execute query, statement={}", statement);
        final PreparedStatement pstmt = connection.prepareStatement(statement);
        if (data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                pstmt.setString(i + 1, data[i]);
            }
        }
        pstmt.executeUpdate();
    }

    private void executeQuery(Connection connection, String statement) throws SQLException {
        executeQuery(connection, statement, new String[]{});
    }

}
