package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.cache.TableCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.CreateForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaTableLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.unique.Unique;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationPeers;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatabaseReplicaService {
    private final MetadataMapper mapper;
    private final ReplicationPeers peers;
    private final UserService users;
    private final ReplicationNotificationOutboxService outbox;
    private final ReplicationNotificationDispatcher dispatcher;
    private final DatabaseCacheRepository databases;
    private final TableCacheRepository tables;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${dbrepo.baseUrl}")
    private String baseUrl;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Transactional
    public void request(Database detached, String url) {
        final Database database = entityManager.find(Database.class, detached.getId(), LockModeType.PESSIMISTIC_READ);
        if (database == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Database not found");
        final String target = validate(database, url);
        if (hasTarget(database, target)) return;
        orderedTables(database);
        queue(ReplicationNotificationType.DATABASE_PREPARE,
                "/api/replication/database/" + database.getId() + "/prepare", new AddReplicaDto(target), database.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(UUID databaseId, String url, Set<UUID> preparedTableIds) throws UserNotFoundException, NotAllowedException {
        final Database database = entityManager.find(Database.class, databaseId, LockModeType.PESSIMISTIC_WRITE);
        if (database == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Database not found");
        final String target = validate(database, url);
        if (!preparedTableIds.containsAll(database.getTables().stream().map(Table::getId).toList())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table inventory changed during preparation");
        }
        if (hasTarget(database, target)) return;
        final var owner = users.findByUsername(database.getOwnedBy());
        final List<Table> ordered = orderedTables(database);
        final ReplicaLocation location = ReplicaLocation.builder().url(target).build();
        database.getReplicaUrls().add(location);
        database.setCreationLocation(baseUrl);
        final List<TableNotificationDto> tableNotifications = new ArrayList<>();
        for (Table table : ordered) {
            addKeyMetadata(database, table);
            table.getReplicaUrls().add(ReplicaTableLocation.builder().url(target).build());
            table.setCreationLocation(baseUrl);
            tableNotifications.add(TableNotificationDto.builder().databaseId(databaseId).creationId(table.getId())
                    .createTableDto(tablePayload(table)).replicas(List.of(location)).build());
        }
        final var create = CreateDatabaseDto.builder().name(database.getName()).cid(database.getContainer().getId())
                .isPublic(database.getIsPublic()).isSchemaPublic(database.getIsSchemaPublic()).creationLocation(baseUrl)
                .replicaUrls(database.getReplicaUrls().stream().map(ReplicaLocation::getUrl).toList()).build();
        final var notification = DatabaseNotificationDto.builder().creationId(databaseId).createDatabaseDto(create)
                .owner(ReplicationOwnerDto.builder().siteUrl(baseUrl).issuer(issuer)
                        .subject(owner.getId().toString()).username(owner.getUsername()).build()).build();
        final var views = database.getViews().stream()
                .sorted(Comparator.comparing(at.ac.tuwien.ifs.dbrepo.core.entity.database.View::getCreated,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(view -> ViewNotificationDto.builder().databaseId(databaseId).creationId(view.getId())
                        .viewDto(mapper.viewToViewDto(view)).replicas(List.of(location)).build()).toList();
        final var bootstrap = new DatabaseBootstrapDto(DatabaseBootstrapDto.idFor(databaseId, target), target,
                notification, tableNotifications, views);
        afterCommit(() -> {
            databases.deleteById(databaseId);
            ordered.forEach(table -> tables.deleteById(table.getId()));
        });
        queue(ReplicationNotificationType.DATABASE_BOOTSTRAP, "/api/replication/database/bootstrap", bootstrap, databaseId);
    }

    private String validate(Database database, String url) {
        if (ReplicationSites.isReplica(database.getCreationLocation(), baseUrl)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Add replica sites on the primary database");
        }
        final String target;
        try {
            target = peers.requireAllowedSite(url);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        if (!ReplicationSites.isReplica(target, baseUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot replicate to the source site");
        }
        return target;
    }

    private boolean hasTarget(Database database, String target) {
        return database.getReplicaUrls().stream().anyMatch(r -> target.equals(r.getUrl()));
    }

    private void queue(ReplicationNotificationType type, String path, Object payload, UUID databaseId) {
        final var entry = outbox.enqueue(type, HttpMethod.POST, path, payload, databaseId);
        afterCommit(() -> dispatcher.dispatchAsync(entry.getId()));
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }

    // Foreign-key parents must exist before the ordinary table-create endpoint can create their children.
    private List<Table> orderedTables(Database database) {
        final List<Table> remaining = new ArrayList<>(database.getTables());
        final List<Table> ordered = new ArrayList<>();
        final Set<UUID> ready = new HashSet<>();
        while (!remaining.isEmpty()) {
            final List<Table> next = remaining.stream().filter(table -> table.getConstraints().getForeignKeys().stream()
                    .allMatch(fk -> fk.getReferencedTable().getId().equals(table.getId())
                            || ready.contains(fk.getReferencedTable().getId()))).toList();
            if (next.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot bootstrap cyclic or external foreign-key dependencies");
            next.forEach(table -> { ordered.add(table); ready.add(table.getId()); });
            remaining.removeAll(next);
        }
        return ordered;
    }

    private void addKeyMetadata(Database database, Table table) {
        if (table.getColumns().stream().anyMatch(c -> "replication_key".equals(c.getInternalName()))) return;
        final var column = mapper.columnCreateDtoToTableColumn(CreateTableColumnDto.builder().name("replication_key")
                .type(ColumnTypeDto.VARCHAR).size(36L).nullAllowed(false).description("Replication key").build(),
                database.getContainer().getImage());
        column.setTable(table);
        column.setOrdinalPosition(table.getColumns().stream().mapToInt(TableColumn::getOrdinalPosition).max().orElse(-1) + 1);
        table.getColumns().add(column);
        table.getConstraints().getUniques().add(Unique.builder().name("replication_key").table(table)
                .columns(List.of(column)).build());
    }

    private CreateTableDto tablePayload(Table table) {
        final var dto = mapper.tableToTableDto(table);
        final var columns = dto.getColumns().stream().sorted(Comparator.comparing(c -> c.getOrdinalPosition()))
                .map(c -> CreateTableColumnDto.builder().name(c.getName()).type(c.getColumnType()).size(c.getSize())
                        .d(c.getD()).indexLength(c.getIndexLength()).nullAllowed(c.getIsNullAllowed())
                        .description(c.getDescription()).conceptUri(c.getConceptUri()).unitUri(c.getUnit())
                        .enums(c.getEnums() == null ? null : c.getEnums().stream().map(e -> e.getValue()).toList())
                        .sets(c.getSets() == null ? null : c.getSets().stream().map(s -> s.getValue()).toList())
                        .build()).toList();
        final var constraints = table.getConstraints();
        return CreateTableDto.builder().name(table.getName()).description(table.getDescription())
                .isPublic(table.getIsPublic()).isSchemaPublic(table.getIsSchemaPublic()).creationLocation(baseUrl)
                .columns(columns).constraints(CreateTableConstraintsDto.builder().checks(constraints.getChecks())
                        .uniques(constraints.getUniques().stream().map(u -> u.getColumns().stream()
                                .map(TableColumn::getInternalName).toList()).toList())
                        .primaryKey(constraints.getPrimaryKey().stream().map(p -> p.getColumn().getInternalName())
                                .collect(Collectors.toSet()))
                        .foreignKeys(dto.getConstraints().getForeignKeys().stream().map(fk -> CreateForeignKeyDto.builder()
                                .referencedTable(fk.getReferencedTable().getInternalName())
                                .columns(fk.getReferences().stream().map(r -> r.getColumn().getInternalName()).toList())
                                .referencedColumns(fk.getReferences().stream().map(r -> r.getReferencedColumn().getInternalName()).toList())
                                .onUpdate(fk.getOnUpdate()).onDelete(fk.getOnDelete()).build()).toList()).build()).build();
    }
}
