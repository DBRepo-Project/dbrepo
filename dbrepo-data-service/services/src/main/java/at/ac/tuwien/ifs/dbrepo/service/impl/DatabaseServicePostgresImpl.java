package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStoreCreateException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.mapper.PostgresMapper;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
public class DatabaseServicePostgresImpl extends DataConnector implements DatabaseService {

    private final PostgresMapper mariaDbMapper;

    @Autowired
    public DatabaseServicePostgresImpl(PostgresMapper mariaDbMapper) {
        this.mariaDbMapper = mariaDbMapper;
    }

    @Override
    public Database create(Container container, CreateDatabaseDto data) throws DatabaseMalformedException,
            SQLException {
        final ComboPooledDataSource dataSource = getDataSource(container);
        final Connection connection = dataSource.getConnection();
        try {
            /* create database if not exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseCreateDatabaseQuery(data.getInternalName()))
                    .execute();
            log.atDebug()
                    .setMessage("created database: " + data.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "create_database")
                    .log();
        } catch (SQLException e) {
            log.error("Failed to create database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to create database access: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created database with name {}", data.getInternalName());
        return Database.builder()
                .internalName(data.getInternalName())
                .container(container)
                .build();
    }

    @Override
    public void createExtensions(Container container, String databaseName) throws SQLException,
            QueryStoreCreateException {
        final ComboPooledDataSource dataSource = getDataSource(container, databaseName);
        final Connection connection = dataSource.getConnection();
        try {
            /* create query store */
            for (String extension : List.of("plpython3u", "pgcrypto", "aws_s3", "periods", "dbrepo")) {
                final long start = System.currentTimeMillis();
                connection.prepareStatement(mariaDbMapper.createExtensionRawQuery(extension))
                        .execute();
                log.atDebug()
                        .setMessage("created extension " + extension + " in database: " + databaseName)
                        .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                        .addKeyValue(Constants.ACTION, "create_extension")
                        .log();
            }
        } catch (SQLException e) {
            log.error("Failed to create extension: {}", e.getMessage());
            throw new QueryStoreCreateException("Failed to create extension: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created extensions in database with name {}", databaseName);
    }

    @Override
    public void update(Database database, UpdateUserPasswordDto data) throws SQLException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* update user password */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseSetPasswordQuery(data.getUsername(), data.getPassword()))
                    .execute();
            log.atDebug()
                    .setMessage("updated user password: " + database.getInternalName())
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "update_user_password")
                    .log();
        } catch (SQLException e) {
            log.error("Failed to update user password in database: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to update user password in database: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated user password in database with id {}", database.getId());
    }
}
