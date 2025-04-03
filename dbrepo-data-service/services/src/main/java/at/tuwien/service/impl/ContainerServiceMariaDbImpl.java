package at.tuwien.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.tuwien.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStoreCreateException;
import at.tuwien.mapper.MariaDbMapper;
import at.tuwien.service.ContainerService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

@Log4j2
@Service
public class ContainerServiceMariaDbImpl extends DataConnector implements ContainerService {

    private final RabbitConfig rabbitConfig;
    private final MariaDbMapper mariaDbMapper;

    @Autowired
    public ContainerServiceMariaDbImpl(RabbitConfig rabbitConfig, MariaDbMapper mariaDbMapper) {
        this.rabbitConfig = rabbitConfig;
        this.mariaDbMapper = mariaDbMapper;
    }

    @Override
    public DatabaseDto createDatabase(ContainerDto container, CreateDatabaseDto data) throws SQLException,
            DatabaseMalformedException {
        final ComboPooledDataSource dataSource = getDataSource(container);
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
        return DatabaseDto.builder()
                .internalName(data.getInternalName())
                .exchangeName(rabbitConfig.getExchangeName())
                .owner(UserBriefDto.builder()
                        .id(data.getUserId())
                        .build())
                .contact(UserBriefDto.builder()
                        .id(data.getUserId())
                        .build())
                .container(container)
                .build();
    }

    @Override
    public void createQueryStore(ContainerDto container, String databaseName) throws SQLException,
            QueryStoreCreateException {
        final ComboPooledDataSource dataSource = getDataSource(container, databaseName);
        final Connection connection = dataSource.getConnection();
        try {
            /* create query store */
            long start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateTableRawQuery())
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateHashTableProcedureRawQuery())
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateStoreQueryProcedureRawQuery())
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            connection.prepareStatement(mariaDbMapper.queryStoreCreateInternalStoreQueryProcedureRawQuery())
                    .execute();
            log.trace("executed statement in {} ms", System.currentTimeMillis() - start);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            log.error("Failed to create query store: {}", e.getMessage());
            throw new QueryStoreCreateException("Failed to create query store: " + e.getMessage(), e);
        } finally {
            dataSource.close();
        }
        log.info("Created query store in database with name {}", databaseName);
    }
}
