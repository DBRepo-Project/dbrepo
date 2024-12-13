package at.tuwien.service.impl;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.config.RabbitConfig;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.service.DatabaseService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

@Log4j2
@Service
public class DatabaseServiceMariaDbImpl extends HibernateConnector implements DatabaseService {

    private final RabbitConfig rabbitConfig;
    private final MariaDbMapper mariaDbMapper;

    @Autowired
    public DatabaseServiceMariaDbImpl(RabbitConfig rabbitConfig, MariaDbMapper mariaDbMapper) {
        this.rabbitConfig = rabbitConfig;
        this.mariaDbMapper = mariaDbMapper;
    }

    @Override
    public PrivilegedDatabaseDto create(PrivilegedContainerDto container, CreateDatabaseDto data) throws SQLException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(container, null);
        final Connection connection = dataSource.getConnection();
        try {
            /* create database if not exists */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseCreateDatabaseQuery(data.getInternalName()))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create database access: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to create database access: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created database with name {}", data.getInternalName());
        return PrivilegedDatabaseDto.builder()
                .internalName(data.getInternalName())
                .exchangeName(rabbitConfig.getExchangeName())
                .creator(UserDto.builder()
                        .id(data.getUserId())
                        .build())
                .owner(UserDto.builder()
                        .id(data.getUserId())
                        .build())
                .contact(UserDto.builder()
                        .id(data.getUserId())
                        .build())
                .container(container)
                .build();
    }

    @Override
    public void update(PrivilegedDatabaseDto database, UpdateUserPasswordDto data) throws SQLException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database);
        final Connection connection = dataSource.getConnection();
        try {
            /* update user password */
            final long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.databaseSetPasswordQuery(data.getUsername(), data.getPassword()))
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to update user password in database: {}", e.getMessage());
            throw new DatabaseMalformedException("Failed to update user password in database: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Updated user password in database with id {}", database.getId());
    }
}
