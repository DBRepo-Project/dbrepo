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
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
public class ReplicationServiceImpl implements ReplicationService {

    private final RestTemplate replicationRestTemplate;

    public ReplicationServiceImpl(@Qualifier("replicationRestTemplate") RestTemplate replicationRestTemplate) {
        this.replicationRestTemplate = replicationRestTemplate;
    }

    @Override
    public void replicateTuple(TupleWithTimestampsDto tuple, Database database, Table table) {
        send(tuple, database, table, HttpMethod.POST);
    }

    @Override
    public void replicateTupleUpdate(TupleWithTimestampsDto tuple, Database database, Table table) {
        send(tuple, database, table, HttpMethod.PUT);
    }

    @Override
    public void replicateTupleDelete(TupleWithTimestampsDto tuple, Database database, Table table) {
        send(tuple, database, table, HttpMethod.DELETE);
    }

    private void send(TupleWithTimestampsDto tuple, Database database, Table table, HttpMethod method) {
        if (tuple == null || tuple.getReplicationKey() == null) {
            log.warn("Skip tuple replication for {}.{}: missing replication key", database.getInternalName(),
                    table.getInternalName());
            return;
        }
        try {
            final DataReplicationDto request = DataReplicationDto.builder()
                    .tuple(tuple)
                    .database(toDatabaseDto(database))
                    .table(toTableDto(database, table))
                    .build();
            final ResponseEntity<Void> response = replicationRestTemplate.exchange("/api/replication/data",
                    method, new HttpEntity<>(request), Void.class);
            log.info("Sent {} tuple replication for {}.{} key {}: {}", method, database.getInternalName(),
                    table.getInternalName(), tuple.getReplicationKey(), response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send {} tuple replication for {}.{} key {}: {}", method,
                    database.getInternalName(), table.getInternalName(), tuple.getReplicationKey(), e.getMessage(),
                    e);
        }
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
                .creationLocation(database.getCreationLocation())
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
                .creationLocation(table.getCreationLocation())
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
