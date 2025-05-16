package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDetailsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public abstract class AbstractEndpoint {

    public boolean hasRole(Principal principal, String role) {
        if (principal == null || role == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    public boolean isSystem(Principal principal) {
        if (principal == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("system"));
    }

    public UUID getId(Principal principal) {
        if (principal == null) {
            return null;
        }
        final Authentication authentication = (Authentication) principal;
        if (authentication.getPrincipal() instanceof UserDetailsDto user) {
            if (user.getId() == null) {
                throw new IllegalArgumentException("Principal has no id");
            }
            return UUID.fromString(user.getId());
        }
        throw new IllegalArgumentException("Unknown principal instance: " + authentication.getPrincipal().getClass());
    }

    public void removeInternalData(ContainerDto container) {
        container.setPassword(null);
        container.setUsername(null);
        container.setHost(null);
        container.setPort(null);
    }

    public Database filterDatabase(Database database, Principal principal) throws NotAllowedException {
        if (principal != null) {
            if (isSystem(principal)) {
                log.trace("filter database: system principal, skip");
                return database;
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getUser().getId().equals(getId(principal)))
                    .findFirst();
            if (!database.getIsPublic() && !database.getIsSchemaPublic() && optional.isEmpty()) {
                log.error("Failed to find database: not public and no access found");
                throw new NotAllowedException("Failed to find database: not public and no access found");
            }
            /* reduce metadata */
            if (!database.getOwner().getId().equals(getId(principal))) {
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
            if (isSystem(principal)) {
                return views;
            }
            final Optional<DatabaseAccess> optional = database.getAccesses()
                    .stream()
                    .filter(a -> a.getHdbid().equals(getId(principal)))
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
