package at.tuwien.service.impl;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseConnectionException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryStoreService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;

@Log4j2
@Service
public class QueryStoreServiceImpl extends HibernateConnector implements QueryStoreService {

    private final DatabaseService databaseService;

    @Autowired
    public QueryStoreServiceImpl(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    public void create(Long containerId, Long databaseId) throws DatabaseNotFoundException, DatabaseConnectionException {
        final Database database = databaseService.findById(containerId, databaseId);
        /* create */
        final Connection connection = getConnection(database.getContainer().getImage(), database.getContainer(), database);
        execute(connection, "CREATE SEQUENCE IF NOT EXISTS `qs_queries_seq`");
        execute(connection, "CREATE SEQUENCE IF NOT EXISTS `qs_tables_seq`");
        execute(connection, "CREATE SEQUENCE IF NOT EXISTS `qs_columns_seq`");
        execute(connection, "CREATE TABLE `qs_queries` (`id` bigint not null primary key default nextval(`qs_queries_seq`), `cid` bigint not null, `created` datetime not null, `created_by` bigint not null, `dbid` bigint not null, `execution` datetime not null, `last_modified` datetime not null, `query` text not null, `query_hash` varchar(255) not null, `result_hash` varchar(255), `result_number` bigint)");
        execute(connection, "CREATE TABLE `qs_tables` (`id` bigint not null primary key default nextval(`qs_tables_seq`), `created` datetime not null, `dbid` bigint not null, `last_modified` datetime)");
        execute(connection, "CREATE TABLE `qs_columns` (`id` bigint not null primary key default nextval(`qs_columns_seq`), `created` datetime not null, `dbid` bigint not null, `tid` bigint not null, `last_modified` datetime)");
        log.info("Created query store in database with id {}", databaseId);
        log.debug("created query store in database {}", database);
    }

}
