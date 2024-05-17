package at.tuwien.service.impl;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.user.PrivilegedUserDto;
import at.tuwien.exception.*;
import at.tuwien.service.AccessService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

@Log4j2
@Service
public class AccessServiceMariaDbImpl extends HibernateConnector implements AccessService {

    @Value("${dbrepo.grant.default.read}")
    private String grantDefaultRead;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    @Override
    public void create(PrivilegedDatabaseDto database, PrivilegedUserDto user, AccessTypeDto access)
            throws SQLException, DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* create user if not exists */
            connection.prepareStatement("CREATE USER IF NOT EXISTS `" + user.getUsername() + "`@`%` IDENTIFIED BY PASSWORD '" + user.getPassword() + "';")
                    .execute();
            /* grant access */
            final String grants = access != AccessTypeDto.READ ? grantDefaultWrite : grantDefaultRead;
            connection.prepareStatement("GRANT " + grants + " ON *.* TO `" + user.getUsername() + "`@`%`;")
                    .execute();
            /* grant query store */
            connection.prepareStatement("GRANT EXECUTE ON PROCEDURE `store_query` TO `" + user.getUsername() + "`@`%`;")
                    .execute();
            /* apply access rights */
            connection.prepareStatement("FLUSH PRIVILEGES;");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to give database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to give database access: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created access to database with internal name {} for user with id {}", database.getInternalName(),
                user.getId());
    }

    @Override
    public void update(PrivilegedDatabaseDto database, PrivilegedUserDto user, AccessTypeDto access)
            throws DatabaseMalformedException, SQLException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* grant access */
            connection.prepareStatement("GRANT SELECT" +
                            (access != AccessTypeDto.READ ? "CREATE, CREATE VIEW, CREATE ROUTINE, CREATE TEMPORARY TABLES, LOCK TABLES, INDEX, TRIGGER, INSERT, UPDATE, DELETE" : "") +
                            " ON *.* TO `" + user.getUsername() + "`@`%`;")
                    .execute();
            /* apply access rights */
            connection.prepareStatement("FLUSH PRIVILEGES;");
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
    public void delete(PrivilegedDatabaseDto database, PrivilegedUserDto user) throws DatabaseMalformedException,
            SQLException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* revoke access */
            connection.prepareStatement("REVOKE ALL PRIVILEGES ON *.* FROM `" + user.getUsername() + "`@`%`;")
                    .execute();
            /* apply access rights */
            connection.prepareStatement("FLUSH PRIVILEGES;");
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
