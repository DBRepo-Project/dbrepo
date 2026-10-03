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
            statement.execute();
            log.atDebug()
                    .setMessage("create user in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_user")
                    .log();
            /* grant access */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseGrantPrivilegesQuery(database.getInternalName(), user.getUsername(), grants))
                    .execute();
            log.atDebug()
                    .setMessage("grant user privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, Constants.GRANT_USER_PRIVILEGES)
                    .log();
            /* grant query store */
            start = System.currentTimeMillis();
            if (!isReplicatedDatabase(database)) {
                connection.prepareStatement(mariaDbMapper.databaseGrantProcedureQuery(user.getUsername(), "store_query"))
                        .execute();
            }
            log.atDebug()
                    .setMessage("grant procedure privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "grant_procedure_privileges")
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
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to give database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to give database access: " + e.getMessage(), e);
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
            connection.prepareStatement(mariaDbMapper.databaseRevokePrivilegesQuery(database.getInternalName(), user.getUsername()))
                    .execute();
            if (isReplicatedDatabase(database) && !isServiceUser(database, user)) {
                revokeQueryStore(connection, database, user);
            }
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

    private void revokeQueryStore(Connection connection, Database database, User user) throws SQLException {
        // Older installations also granted EXECUTE at procedure scope, independently of db.*.
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM mysql.procs_priv "
                + "WHERE Db = ? AND User = ? AND Host = '%' AND Routine_name = 'store_query' "
                + "AND Routine_type = 'PROCEDURE' AND FIND_IN_SET('Execute', Proc_priv) > 0")) {
            statement.setString(1, database.getInternalName());
            statement.setString(2, user.getUsername());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    try (PreparedStatement revoke = connection.prepareStatement(
                            mariaDbMapper.databaseRevokeProcedureQuery(database.getInternalName(), user.getUsername(), "store_query"))) {
                        revoke.execute();
                    }
                }
            }
        }
    }

    @Override
    public void delete(Database database, User user) throws DatabaseMalformedException,
            SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
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
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to revoke database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted access to database with id {} for user with id {}", database.getId(), user.getId());
    }

}
