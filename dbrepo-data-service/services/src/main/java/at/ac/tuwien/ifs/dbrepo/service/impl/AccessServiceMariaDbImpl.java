package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
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
public class AccessServiceMariaDbImpl extends DataConnector implements AccessService {

    @Value("${dbrepo.grant.default.read}")
    private String grantDefaultRead;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    private final MariaDbMapper mariaDbMapper;

    @Autowired
    public AccessServiceMariaDbImpl(MariaDbMapper mariaDbMapper) {
        this.mariaDbMapper = mariaDbMapper;
    }

    @Override
    public void create(DatabaseDto database, UserDto user, AccessTypeDto access)
            throws SQLException, DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create user if not exists */
            long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseCreateUserQuery(user.getUsername(), user.getPassword()))
                    .execute();
            log.atDebug()
                    .setMessage("create user in database: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_user")
                    .log();
            /* grant access */
            final String grants = access != AccessTypeDto.READ ? grantDefaultWrite : grantDefaultRead;
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
            connection.prepareStatement(mariaDbMapper.databaseGrantProcedureQuery(user.getUsername(), "store_query"))
                    .execute();
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
    public void update(DatabaseDto database, UserDto user, AccessTypeDto access) throws DatabaseMalformedException,
            SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* grant access */
            final String grants = access != AccessTypeDto.READ ? grantDefaultWrite : grantDefaultRead;
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

    @Override
    public void delete(DatabaseDto database, UserDto user) throws DatabaseMalformedException,
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
