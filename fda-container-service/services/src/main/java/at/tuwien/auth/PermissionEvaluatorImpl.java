package at.tuwien.auth;

import at.tuwien.entities.container.Container;
import at.tuwien.repository.jpa.ContainerRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.security.Principal;
import java.util.Optional;

@Component
@Log4j2
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    private final ContainerRepository containerRepository;

    @Autowired
    public PermissionEvaluatorImpl(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    @Override
    public boolean hasPermission(Authentication a, Object t, Object p) {
        if (a == null) {
            log.error("Failed to evaluate authentication");
            log.debug("failed to evaluate authentication, it is null");
            return false;
        }
        final Principal principal = (Principal) a.getPrincipal();
        if (!(t instanceof Long)) {
            log.error("Failed to evaluate target domain object");
            log.debug("failed to evaluate target domain object {} is not of type Long", t);
            return false;
        }
        final Long id = (Long) t;
        if (!(p instanceof String)) {
            log.error("Failed to evaluate permission");
            log.debug("failed to evaluate permission {} is not of type String", p);
            return false;
        }
        final String permission = String.valueOf(p);
        final Optional<Container> optional = containerRepository.findById(id);
        switch (permission) {
            case "DELETE_CONTAINER":
                if (optional.isEmpty()) {
                    log.error("Failed to grant permission {}", permission);
                    log.debug("failed to grant permission {}, container is not present", permission);
                    return false;
                }
                final Container container = optional.get();
                if (!container.getCreator().getUsername().equals(principal.getName())) {
                    log.error("Failed to grant permission {}", permission);
                    log.debug("failed to grant permission {}, owner is not the current user", permission);
                    return false;
                } else if (container.getDatabases().size() > 0) {
                    log.error("Failed to grant permission {}", permission);
                    log.debug("failed to grant permission {}, databases present in the container", permission);
                    return false;
                }
                return true;
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) {
        return false;
    }
}
