package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.PostgresMapper;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
@Service
public class AccessServicePostgresImpl extends DataConnector implements AccessService {

    @Value("${dbrepo.grant.default.read}")
    private String grantDefaultRead;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    private final PostgresMapper mariaDbMapper;

    @Autowired
    public AccessServicePostgresImpl(PostgresMapper mariaDbMapper) {
        this.mariaDbMapper = mariaDbMapper;
    }

    @Override
    public void create(Database database, AccessTypeDto access, String username, String password) throws SQLException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create user if not exists */
            long start = System.currentTimeMillis();
            final PreparedStatement statement;
            statement = connection.prepareStatement(mariaDbMapper.databaseCreateUserQuery(username, password));
            int affectedRows = statement.executeUpdate();
            log.atDebug()
                    .setMessage(affectedRows > 0 ? "created" : "did not create" + " user: " + username)
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_ignore_role")
                    .log();
            /* grant access */
            final String grants = access != AccessTypeDto.READ ? grantDefaultWrite : grantDefaultRead;
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseGrantPrivilegesQuery(database.getInternalName(), username, grants))
                    .execute();
            log.atDebug()
                    .setMessage("grant user privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, Constants.GRANT_USER_PRIVILEGES)
                    .log();
        } catch (SQLException e) {
            log.error("Failed to give database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to give database access: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created access to database with internal name {} for user: {}", database.getInternalName(),
                username);
    }

    @Override
    public void update(Database database, AccessTypeDto access, String username) throws DatabaseMalformedException,
            SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* grant access */
            final String grants = access != AccessTypeDto.READ ? grantDefaultWrite : grantDefaultRead;
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseGrantPrivilegesQuery(database.getInternalName(), username, grants))
                    .execute();
            log.atDebug()
                    .setMessage("update privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, Constants.GRANT_USER_PRIVILEGES)
                    .log();
        } catch (SQLException e) {
            log.error("Failed to modify database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to modify database access: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated access to database with id {} for user with username {}", database.getId(), username);
    }

    @Override
    public void delete(Database database, String username) throws DatabaseMalformedException,
            SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* revoke access */
            long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseRevokePrivilegesQuery(database.getInternalName(), username))
                    .execute();
            log.atDebug()
                    .setMessage("revoke privileges in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, Constants.GRANT_USER_PRIVILEGES)
                    .log();
        } catch (SQLException e) {
            log.error("Failed to revoke database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to execute query: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Deleted access to database with id {} for user with username {}", database.getId(), username);
    }

}
