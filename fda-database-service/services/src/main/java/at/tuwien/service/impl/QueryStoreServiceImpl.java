package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseConnectionException;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryStoreService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public void create(Long containerId, Long databaseId) throws DatabaseNotFoundException, DatabaseConnectionException, DatabaseMalformedException {
        final Database database = databaseService.findById(containerId, databaseId);
        /* create */
        final ComboPooledDataSource dataSource = getDataSource(database.getContainer().getImage(), database.getContainer(), database);
        try {
            final Connection connection = dataSource.getConnection();
            final PreparedStatement preparedStatement10 = connection.prepareStatement("CREATE SEQUENCE IF NOT EXISTS `qs_queries_seq`");
            preparedStatement10.executeUpdate();
            final PreparedStatement preparedStatement11 = connection.prepareStatement("CREATE SEQUENCE IF NOT EXISTS `qs_tables_seq`");
            preparedStatement11.executeUpdate();
            final PreparedStatement preparedStatement12 = connection.prepareStatement("CREATE SEQUENCE IF NOT EXISTS `qs_columns_seq`");
            preparedStatement12.executeUpdate();
            final PreparedStatement preparedStatement13 = connection.prepareStatement("CREATE SEQUENCE IF NOT EXISTS `qs_views_seq`");
            preparedStatement13.executeUpdate();
            final PreparedStatement preparedStatement20 = connection.prepareStatement("CREATE TABLE `qs_queries` (`id` bigint not null primary key default nextval(`qs_queries_seq`), `cid` bigint not null, `created` datetime not null default now(), `created_by` bigint not null, `dbid` bigint not null, `execution` datetime not null, `last_modified` datetime, `query` text not null,  `query_normalized` text not null, `query_hash` varchar(255) not null, `type` varchar(10) not null, `result_hash` varchar(255), `result_number` bigint)");
            preparedStatement20.executeUpdate();
            final PreparedStatement preparedStatement21 = connection.prepareStatement("CREATE TABLE `qs_tables` (`id` bigint not null primary key default nextval(`qs_tables_seq`), `created` datetime not null, `dbid` bigint not null, `last_modified` datetime)");
            preparedStatement21.executeUpdate();
            final PreparedStatement preparedStatement22 = connection.prepareStatement("CREATE TABLE `qs_columns` (`id` bigint not null primary key default nextval(`qs_columns_seq`), `created` datetime not null, `dbid` bigint not null, `tid` bigint not null, `last_modified` datetime)");
            preparedStatement22.executeUpdate();
            final PreparedStatement preparedStatement23 = connection.prepareStatement("CREATE TABLE `qs_views` ( `id` bigint not null primary key default nextval(`qs_views_seq`), `vdbid` bigint not null, `created_by` bigint not null, `name` varchar(255) not null, `is_public` boolean not null, `is_initial_view` boolean not null, `query` text not null, `created` datetime not null)");
            preparedStatement23.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to create database {}: {}", database, e.getMessage());
            throw new DatabaseMalformedException("Failed to execute and map time-versioned query", e);
        } finally {
            dataSource.close();
        }
        log.info("Created query store in database with id {}", databaseId);
        log.debug("created query store in database {}", database);
    }

}
