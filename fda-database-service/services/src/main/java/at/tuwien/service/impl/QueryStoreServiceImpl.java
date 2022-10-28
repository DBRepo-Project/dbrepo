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
            executeQuery(connection, "CREATE SEQUENCE IF NOT EXISTS `qs_queries_seq`");
            executeQuery(connection, "CREATE SEQUENCE IF NOT EXISTS `qs_tables_seq`");
            executeQuery(connection, "CREATE SEQUENCE IF NOT EXISTS `qs_columns_seq`");
            executeQuery(connection, "CREATE SEQUENCE IF NOT EXISTS `qs_views_seq`");
            executeQuery(connection, "CREATE TABLE `qs_queries` (`id` bigint not null primary key default nextval(`qs_queries_seq`), `cid` bigint not null, `created` datetime not null default now(), `created_by` bigint not null, `dbid` bigint not null, `execution` datetime not null, `last_modified` datetime, `query` text not null, `query_normalized` text not null, `is_persisted` boolean not null, `query_hash` varchar(255) not null, `result_hash` varchar(255), `result_number` bigint, `type` VARCHAR(50) not null)");
            executeQuery(connection, "CREATE TABLE `qs_tables` (`id` bigint not null primary key default nextval(`qs_tables_seq`), `created` datetime not null, `dbid` bigint not null, `last_modified` datetime)");
            executeQuery(connection, "CREATE TABLE `qs_columns` (`id` bigint not null primary key default nextval(`qs_columns_seq`), `created` datetime not null, `dbid` bigint not null, `tid` bigint not null, `last_modified` datetime)");
            executeQuery(connection, "CREATE TABLE `qs_views` ( `id` bigint not null primary key default nextval(`qs_views_seq`), `vcid` bigint not null, `vdbid` bigint not null, `created_by` bigint not null, `name` varchar(255) not null, `internal_name` varchar(255) not null, `is_public` boolean not null, `is_initial_view` boolean not null, `query` text not null, `created` datetime not null)");
        } catch (SQLException e) {
            log.error("Failed to create database {}, reason: {}", database, e.getMessage());
            throw new DatabaseMalformedException("Failed to create database", e);
        } finally {
            dataSource.close();
        }
        log.info("Created query store in database with id {}", databaseId);
        log.trace("created query store in database {}", database);
    }

    private void executeQuery(Connection connection, String statement) throws SQLException {
        final PreparedStatement pstmt = connection.prepareStatement(statement);
        log.trace("mapped statement {} to prepared statement {}", statement, pstmt);
        pstmt.executeUpdate();
    }

}
