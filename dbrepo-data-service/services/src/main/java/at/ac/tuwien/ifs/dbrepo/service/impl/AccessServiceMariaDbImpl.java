package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Service
public class AccessServiceMariaDbImpl extends DataConnector implements AccessService {

    @Value("${dbrepo.grant.default.read}")
    private String grantDefaultRead;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    @Value("${dbrepo.baseUrl:http://localhost}")
    private String baseUrl;

    @Value("${dbrepo.replication.username}")
    private String replicationUsername;

    private final MariaDbMapper mariaDbMapper;

    @Autowired
    public AccessServiceMariaDbImpl(MariaDbMapper mariaDbMapper) {
        this.mariaDbMapper = mariaDbMapper;
    }

    @Override
    public void create(Database database, User user, AccessTypeDto access)
            throws SQLException, DatabaseMalformedException {
        final String grants = grants(database, user, access);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create user if not exists */
            long start = System.currentTimeMillis();
            final PreparedStatement statement;
            if (user.getPassword().startsWith("*")) {
                log.trace("password is hashed: use normal query");
                statement = connection.prepareStatement(mariaDbMapper.databaseCreateUserQuery());
            } else {
                log.trace("password is not hashed: use raw query");
                statement = connection.prepareStatement(mariaDbMapper.databaseCreateUserRawQuery());
            }
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            log.trace("creating database account for user {}", user.getUsername());
            statement.execute();
            log.atDebug()
                    .setMessage("create user in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_user")
                    .log();
            revokeQueryStoreAccess(connection, database, user);
            /* grant access */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseGrantPrivilegesQuery(database.getInternalName(), user.getUsername(), grants))
                    .execute();
            log.atDebug()
                    .setMessage("grant user privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, Constants.GRANT_USER_PRIVILEGES)
                    .log();
            /* apply access rights */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseFlushPrivilegesQuery());
            log.atDebug()
                    .setMessage("flush privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "flush_privileges")
                    .log();
            connection.commit();
            closeUserConnections(connection, database, user);
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to give database access (SQLState={}, code={})", e.getSQLState(), e.getErrorCode());
            throw new DatabaseMalformedException("Failed to give database access (SQLState=" + e.getSQLState()
                    + ", code=" + e.getErrorCode() + ")");
        } finally {
            dataSource.close();
        }
        log.info("Created access to database with internal name {} for user: {}", database.getInternalName(),
                user.getUsername());
    }

    @Override
    public void update(Database database, User user, AccessTypeDto access) throws DatabaseMalformedException,
            SQLException {
        final String grants = grants(database, user, access);
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            // GRANT is additive; a downgrade must revoke the previous database privileges first.
            try (PreparedStatement revoke = connection.prepareStatement(
                    mariaDbMapper.databaseRevokePrivilegesQuery(database.getInternalName(), user.getUsername()))) {
                try {
                    revoke.execute();
                } catch (SQLException e) {
                    // ER_NONEXISTING_GRANT: nothing to revoke. Permission and other SQL errors must still fail.
                    if (e.getErrorCode() != 1141 || !"42000".equals(e.getSQLState())) throw e;
                }
            }
            revokeQueryStoreAccess(connection, database, user);
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseGrantPrivilegesQuery(database.getInternalName(), user.getUsername(), grants))
                    .execute();
            log.atDebug()
                    .setMessage("update privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, Constants.GRANT_USER_PRIVILEGES)
                    .log();
            /* apply access rights */
            connection.prepareStatement(mariaDbMapper.databaseFlushPrivilegesQuery());
            connection.commit();
            closeUserConnections(connection, database, user);
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to modify database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to modify database access: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated access to database with id {} for user with id {}", database.getId(), user.getId());
    }

    private boolean isReplicaReader(Database database, User user) {
        return ReplicationSites.isReplica(database.getCreationLocation(), baseUrl)
                && !isServiceUser(database, user);
    }

    private boolean isReplicatedDatabase(Database database) {
        return ReplicationSites.isReplica(database.getCreationLocation(), baseUrl)
                || database.getReplicaUrls() != null && !database.getReplicaUrls().isEmpty();
    }

    private boolean isServiceUser(Database database, User user) {
        return user.getUsername().equals(replicationUsername)
                || database.getContainer() != null && user.getUsername().equals(database.getContainer().getUsername());
    }

    private String grants(Database database, User user, AccessTypeDto access) throws DatabaseMalformedException {
        if (isReplicaReader(database, user)) {
            if (access != AccessTypeDto.READ) {
                throw new DatabaseMalformedException("Replicated databases only allow local read access");
            }
            return "SELECT";
        }
        // Direct SQL cannot atomically enqueue replication events or canonical subsets.
        if (isReplicatedDatabase(database) && !isServiceUser(database, user)) {
            return "SELECT";
        }
        return access == AccessTypeDto.READ ? grantDefaultRead : grantDefaultWrite;
    }

    @Override
    public void delete(Database database, User user) throws DatabaseMalformedException,
            SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            revokeQueryStoreAccess(connection, database, user);
            /* revoke access */
            long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseRevokePrivilegesQuery(database.getInternalName(), user.getUsername()))
                    .execute();
            log.atDebug()
                    .setMessage("revoke privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, Constants.GRANT_USER_PRIVILEGES)
                    .log();
            /* apply access rights */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseFlushPrivilegesQuery())
                    .execute();
            log.atDebug()
                    .setMessage("flush privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "flush_privileges")
                    .log();
            connection.commit();
            closeUserConnections(connection, database, user);
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to revoke database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted access to database with id {} for user with id {}", database.getId(), user.getId());
    }

    private void closeUserConnections(Connection connection, Database database, User user) throws SQLException {
        if (user.getUsername().equals(database.getContainer().getUsername())) {
            return;
        }
        // Open sessions cache database privileges until they reconnect.
        // MariaDB cannot bind KILL USER on the server; let its JDBC driver bind the account.
        try (PreparedStatement statement = connection.prepareStatement("/*client prepare*/ KILL USER ?@'%'")) {
            statement.setString(1, user.getUsername());
            statement.execute();
        }
    }

    private void revokeQueryStoreAccess(Connection connection, Database database, User user) throws SQLException {
        // Database-level EXECUTE and routine-specific grants are independent in MariaDB.
        for (String procedure : new String[]{null, "store_query", "_store_query", "hash_query", "hash_table"}) {
            try (PreparedStatement statement = connection.prepareStatement(
                    mariaDbMapper.databaseRevokeExecuteQuery(database.getInternalName(), user.getUsername(), procedure))) {
                statement.execute();
            } catch (SQLException error) {
                // Missing grants and absent routines already deny execution.
                if (error.getErrorCode() != 1141 && error.getErrorCode() != 1305 && error.getErrorCode() != 1403) {
                    throw error;
                }
            }
        }
    }

}
