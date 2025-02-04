package at.tuwien.utils;

import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.mapper.MetadataMapper;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public void createUser(UserCreateDto data) {
        try (Response response = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .create(metadataMapper.userCreateDtoToUserRepresentation(data))) {
            if (response.getStatus() != 201) {
                log.error("Failed to create user: {}", response.getStatus());
            }
        }
        log.debug("Created user {} at auth service", data.getUsername());
    }

    public void deleteUser(String username) {
        final List<UserRepresentation> users = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .search(username);
        if (users.isEmpty()) {
            log.error("Failed to find user");
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
