package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.utils.AuthUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

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
