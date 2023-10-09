package at.tuwien.service.impl;

import at.tuwien.api.CachedConnection;
import at.tuwien.config.RabbitMqConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.mapper.DataMapper;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueueService;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
public class QueueServiceImpl extends HibernateConnector implements QueueService {

    private final DataMapper dataMapper;
    private final RabbitMqConfig rabbitMqConfig;
    private final DatabaseService databaseService;
    private final Map<String, CachedConnection> cachedConnections;

    @Autowired
    public QueueServiceImpl(DataMapper dataMapper, RabbitMqConfig rabbitMqConfig, DatabaseService databaseService) {
        this.dataMapper = dataMapper;
        this.rabbitMqConfig = rabbitMqConfig;
        this.databaseService = databaseService;
        this.cachedConnections = new HashMap<>();
    }

    @Scheduled(fixedRate = 5000)
    @Transactional(readOnly = true)
    public void updateCachedConnections() {
        final Instant threshold = Instant.now().minus(rabbitMqConfig.getConnectionTimeout(), ChronoUnit.MILLIS);
        cachedConnections.entrySet()
                .stream()
                .filter(e -> e.getValue().getLastUsed().isAfter(threshold))
                .forEach(connection -> {
                    connection.getValue().getDataSource().close();
                    cachedConnections.remove(connection.getKey());
                    log.debug("connection for database {} expired", connection.getKey());
                });
    }

    @Override
    @Transactional(readOnly = true)
    public void insert(String database, String table, Map<String, Object> data) throws DatabaseNotFoundException,
            QueryMalformedException, TableNotFoundException {
        /* check if connection can be reused */
        final CachedConnection cachedConnection = getCachedConnection(database);
        cachedConnection.setLastUsed(Instant.now());
        /* run query */
        try {
            final Connection connection = cachedConnection.getDataSource().getConnection();
            final PreparedStatement preparedStatement = dataMapper.rabbitMqTupleToInsertOrUpdateQuery(connection, cachedConnection.getTable(table), data);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to insert/update tuple in database {}: {}", database, e.getMessage());
            throw new QueryMalformedException("Failed to insert/update tuple in database " + database, e);
        }
    }

    @Transactional(readOnly = true)
    public CachedConnection getCachedConnection(String databaseInternalName) throws DatabaseNotFoundException {
        if (this.cachedConnections.containsKey(databaseInternalName)) {
            return this.cachedConnections.get(databaseInternalName);
        }
        /* create */
        final Database database = databaseService.findByInternalName(databaseInternalName);
        final ComboPooledDataSource dataSource = getPrivilegedDataSource(database.getContainer().getImage(),
                database.getContainer(), database);
        final CachedConnection cachedConnection = CachedConnection.builder()
                .dataSource(dataSource)
                .database(database)
                .lastUsed(Instant.now())
                .build();
        this.cachedConnections.put(databaseInternalName, cachedConnection);
        log.info("Established connection and added database {} to cache pool", databaseInternalName);
        return cachedConnection;
    }
}
