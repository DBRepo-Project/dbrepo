package at.tuwien.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.service.AccessService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

@Log4j2
@Service
public class AccessServiceMariaDbImpl extends DataConnector implements AccessService {

    @Value("${dbrepo.grant.default.read}")
    private String grantDefaultRead;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    private MariaDbMapper mariaDbMapper;

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
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            /* grant access */
            final String grants = access != AccessTypeDto.READ ? grantDefaultWrite : grantDefaultRead;
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseGrantPrivilegesQuery(user.getUsername(), grants))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            /* grant query store */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseGrantProcedureQuery(user.getUsername(), "store_query"))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            /* apply access rights */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseFlushPrivilegesQuery());
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
    public void update(DatabaseDto database, UserDto user, AccessTypeDto access)
            throws DatabaseMalformedException, SQLException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* grant access */
            final String grants = access != AccessTypeDto.READ ? grantDefaultWrite : grantDefaultRead;
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseGrantPrivilegesQuery(user.getUsername(), grants))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
            connection.prepareStatement(mariaDbMapper.databaseRevokePrivilegesQuery(user.getUsername()))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            /* apply access rights */
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseFlushPrivilegesQuery())
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
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
