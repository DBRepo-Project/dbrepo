package at.tuwien.utils;

import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.MetadataMapper;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Log4j2
@Component
public class KeycloakUtils {

    private final Keycloak keycloak;
    private final KeycloakConfig keycloakConfig;
    private final MetadataMapper metadataMapper;

    @Autowired
    public KeycloakUtils(Keycloak keycloak, KeycloakConfig keycloakConfig, MetadataMapper metadataMapper) {
        this.keycloak = keycloak;
        this.keycloakConfig = keycloakConfig;
        this.metadataMapper = metadataMapper;
    }

    public void createUser(UUID ldapId, UserCreateDto data) {
        final UserRepresentation user = metadataMapper.userCreateDtoToUserRepresentation(data);
        user.singleAttribute("CUSTOM_ID", ldapId.toString());
        try (Response response = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .create(user)) {
            if (response.getStatus() != 201) {
                log.warn("Failed to create user: {}", response.getStatus());
            }
        }
        log.debug("Created user {} at auth service", data.getUsername());
    }

    public UUID getUserId(String username) throws UserNotFoundException {
        final List<UserRepresentation> users = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .search(username);
        if (users.isEmpty()) {
            throw new UserNotFoundException("Failed to find user: " + username);
        }
        return UUID.fromString(users.get(0).getId());
    }

    public void deleteUser(String username) {
        final List<UserRepresentation> users = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .search(username);
        if (users.isEmpty()) {
            log.warn("Failed to find user");
            return;
        }
        try (Response response = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .delete(users.get(0).getId())) {
            if (response.getStatus() != 200) {
                log.error("Failed to delete user: {}", response.getStatus());
            }
        }
    }
}
