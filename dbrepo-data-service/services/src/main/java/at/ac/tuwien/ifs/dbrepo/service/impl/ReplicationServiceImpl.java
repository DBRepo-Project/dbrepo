package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Column;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.TupleReplicationNotificationDispatcher;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxEntry;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    @Value("${dbrepo.baseUrl}")
    private String baseUrl;

    private final TupleReplicationOutboxService outboxService;
    private final TupleReplicationNotificationDispatcher dispatcher;

    public ReplicationServiceImpl(TupleReplicationOutboxService outboxService,
                                  TupleReplicationNotificationDispatcher dispatcher) {
        this.outboxService = outboxService;
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean isEnabled(Database database, Table table) {
        return !ReplicationSites.isReplica(database.getCreationLocation(), baseUrl)
                && !ReplicationSites.isReplica(table.getCreationLocation(), baseUrl)
                && ((database.getReplicaUrls() != null && !database.getReplicaUrls().isEmpty())
                || (table.getReplicaUrls() != null && !table.getReplicaUrls().isEmpty()));
    }

    @Override
    public void prepare(Connection connection, Database database, Table table) throws SQLException {
        if (isEnabled(database, table)) {
            // MariaDB DDL commits implicitly, so prepare before starting the data transaction.
            if (!connection.getAutoCommit()) {
                throw new SQLException("Prepare tuple replication before the data transaction");
            }
            outboxService.ensureTableExists(connection);
            DataConnector.requireInnoDb(connection, table.getInternalName());
            try (var statement = connection.createStatement()) {
                statement.execute("SET time_zone = '+00:00'");
            }
        }
    }

    @Override
    public UUID enqueue(Connection connection, TupleWithTimestampsDto tuple, Database database, Table table,
                        HttpMethod method) throws SQLException {
        if (!isEnabled(database, table)) {
            return null;
        }
        if (tuple == null || tuple.getReplicationKey() == null) {
            throw new SQLException("Cannot replicate tuple without a replication key");
        }
        return outboxService.enqueue(connection, database, table, method, DataReplicationDto.builder()
                .tuple(tuple)
                .database(toDatabaseDto(database))
                .table(toTableDto(database, table))
                .build()).getId();
    }

    @Override
    public void dispatchCommitted(Database database, List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return;
        }
        try {
            dispatcher.dispatchAsync(database, List.copyOf(eventIds));
        } catch (RuntimeException e) {
            log.error("Failed to schedule committed tuple replication notifications for database {}: {}",
                    database.getInternalName(), e.getMessage(), e);
        }
    }

    @Override
    public List<TupleReplicationOutboxEntry> findOutboxEntries(Database database) throws SQLException {
        return outboxService.findAll(database);
    }

    @Override
    public int retryDueOutboxEntries(Database database) {
        return dispatcher.dispatchDue(database);
    }

    @Override
    public boolean retryOutboxEntry(Database database, UUID id) {
        return dispatcher.dispatch(database, id);
    }

    private DatabaseDto toDatabaseDto(Database database) {
        return DatabaseDto.builder()
                .id(database.getId())
                .internalName(database.getInternalName())
                .isPublic(database.getIsPublic())
                .isSchemaPublic(database.getIsSchemaPublic())
                .isDashboardEnabled(database.getIsDashboardEnabled())
                .container(ContainerDto.builder()
                        .id(database.getContainer().getId())
                        .internalName(database.getContainer().getInternalName())
                        .build())
                .replicaUrls(database.getReplicaUrls())
                .creationLocation(database.getCreationLocation() == null || database.getCreationLocation().isBlank()
                        ? baseUrl : database.getCreationLocation())
                .tables(new LinkedList<>())
                .views(new LinkedList<>())
                .accesses(new LinkedList<>())
                .identifiers(new LinkedList<>())
                .subsets(new LinkedList<>())
                .build();
    }

    private TableDto toTableDto(Database database, Table table) {
        return TableDto.builder()
                .id(table.getId())
                .databaseId(database.getId())
                .internalName(table.getInternalName())
                .name(table.getInternalName())
                .isVersioned(true)
                .isPublic(table.getIsPublic())
                .isSchemaPublic(table.getIsSchemaPublic())
                .columns(toColumnDtos(database, table))
                .replicaUrls(table.getReplicaUrls())
                .creationLocation(table.getCreationLocation() == null || table.getCreationLocation().isBlank()
                        ? baseUrl : table.getCreationLocation())
                .build();
    }

    private List<ColumnDto> toColumnDtos(Database database, Table table) {
        if (table.getColumns() == null) {
            return new LinkedList<>();
        }
        final int[] index = new int[]{0};
        return table.getColumns()
                .stream()
                .map(column -> toColumnDto(database, table, column, index[0]++))
                .toList();
    }

    private ColumnDto toColumnDto(Database database, Table table, Column column, int index) {
        return ColumnDto.builder()
                .id(column.getId())
                .databaseId(database.getId())
                .tableId(table.getId())
                .name(column.getInternalName())
                .internalName(column.getInternalName())
                .ordinalPosition(index)
                .columnType(ColumnTypeDto.valueOf(column.getColumnType().name()))
                .isNullAllowed(true)
                .build();
    }
}
