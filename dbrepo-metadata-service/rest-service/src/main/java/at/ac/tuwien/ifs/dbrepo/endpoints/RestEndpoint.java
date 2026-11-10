package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierStatusTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public abstract class RestEndpoint {

    public Database filterDatabase(Database database, Principal principal) throws NotAllowedException {
        if (principal != null) {
            if (AuthUtil.isSystem(principal)) {
                log.trace("filter database: system principal, skip");
                return database;
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUsername().equals(AuthUtil.getUsername(principal)))
                    .findFirst();
            if (!database.getIsPublic() && !database.getIsSchemaPublic() && optional.isEmpty()) {
                log.error("Failed to find database: not public and no access found");
                throw new NotAllowedException("Failed to find database: not public and no access found");
            }
            /* reduce metadata */
            if (!database.getOwnedBy().equals(AuthUtil.getUsername(principal))) {
                log.trace("authenticated user is not owner: remove access list");
                database.setAccesses(List.of());
            }
            final int tables = database.getTables()
                    .size();
            database.setTables(database.getTables()
                    .stream()
                    .filter(t -> t.getIsPublic() || t.getIsSchemaPublic() || optional.isPresent())
                    .toList());
            log.trace("filtered database tables from {} to {}", tables, database.getTables().size());
            final int views = database.getViews()
                    .size();
            database.setViews(database.getViews()
                    .stream()
                    .filter(v -> v.getIsPublic() || v.getIsSchemaPublic() || optional.isPresent())
                    .toList());
            log.trace("filtered database views from {} to {}", views, database.getViews().size());
            return database;
        }
        if (!database.getIsPublic() && !database.getIsSchemaPublic()) {
            log.error("Failed to find database: not public and not authenticated");
            throw new NotAllowedException("Failed to find database: not public and not authenticated");
        }
        /* reduce metadata */
        database.getTables()
                .removeAll(database.getTables()
                        .stream()
                        .filter(t -> !t.getIsPublic() && !t.getIsSchemaPublic())
                        .toList());
        database.getViews()
                .removeAll(database.getViews()
                        .stream()
                        .filter(v -> !v.getIsPublic() && !v.getIsSchemaPublic())
                        .toList());
        database.setAccesses(List.of());
        return database;
    }

    protected Map<UUID, Set<UUID>> visibleColumns(List<Table> tables) {
        return tables.stream().collect(Collectors.toMap(Table::getId,
                table -> table.getColumns().stream().map(TableColumn::getId).collect(Collectors.toSet())));
    }

    /** Remove hidden references from the detached response, leaving entity relationships intact. */
    protected TableDto filterTableMetadata(TableDto table, Map<UUID, Set<UUID>> columns, Principal principal) {
        table.setIdentifiers(filterIdentifiers(table.getIdentifiers(), principal));
        if (table.getConstraints() == null) {
            return table;
        }
        final var constraints = table.getConstraints();
        if (constraints.getForeignKeys() != null) {
            constraints.setForeignKeys(constraints.getForeignKeys().stream().filter(key ->
                    readableTable(key.getTable(), columns) && readableTable(key.getReferencedTable(), columns) &&
                            key.getReferences() != null && key.getReferences().stream().allMatch(reference ->
                            readableColumn(reference.getColumn(), columns) &&
                                    readableColumn(reference.getReferencedColumn(), columns))).toList());
        }
        if (constraints.getUniques() != null) {
            constraints.setUniques(constraints.getUniques().stream().filter(key ->
                    readableTable(key.getTable(), columns) && key.getColumns() != null &&
                            key.getColumns().stream().allMatch(column -> readableColumn(column, columns))).toList());
        }
        if (constraints.getPrimaryKey() != null) {
            constraints.setPrimaryKey(constraints.getPrimaryKey().stream().filter(key ->
                    readableTable(key.getTable(), columns) && readableColumn(key.getColumn(), columns))
                    .toList());
        }
        return table;
    }

    protected ViewDto filterViewMetadata(ViewDto view, Principal principal) {
        view.setIdentifiers(filterIdentifiers(view.getIdentifiers(), principal));
        return view;
    }

    protected List<IdentifierDto> filterIdentifiers(List<IdentifierDto> identifiers, Principal principal) {
        if (identifiers == null) {
            return List.of();
        }
        if (AuthUtil.isSystem(principal)) {
            return identifiers;
        }
        final String username = principal == null ? null : AuthUtil.getUsername(principal);
        return identifiers.stream().filter(identifier ->
                IdentifierStatusTypeDto.PUBLISHED.equals(identifier.getStatus()) ||
                        (IdentifierStatusTypeDto.DRAFT.equals(identifier.getStatus()) && username != null &&
                                identifier.getOwner() != null && username.equals(identifier.getOwner().getUsername())))
                .toList();
    }

    private boolean readableTable(TableBriefDto table, Map<UUID, Set<UUID>> columns) {
        return table != null && columns.containsKey(table.getId());
    }

    private boolean readableColumn(ColumnBriefDto column, Map<UUID, Set<UUID>> columns) {
        return column != null && columns.getOrDefault(column.getTableId(), Set.of()).contains(column.getId());
    }

    public List<View> filterViews(Database database, Principal principal) {
        final List<View> views = database.getViews();
        DatabaseAccess access = null;
        if (principal != null) {
            if (AuthUtil.isSystem(principal)) {
                return views;
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUsername().equals(AuthUtil.getUsername(principal)))
                    .findFirst();
            if (optional.isPresent()) {
                access = optional.get();
            }
        }
        final Boolean hasAccess = access != null;
        return views.stream()
                .filter(v -> v.getIsPublic() || v.getIsSchemaPublic() || hasAccess)
                .toList();
    }

}
