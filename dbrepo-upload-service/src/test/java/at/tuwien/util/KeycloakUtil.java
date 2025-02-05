package at.tuwien.util;

import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.mapper.MetadataMapper;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class KeycloakUtil {


    private final MetadataMapper metadataMapper;
    private final Keycloak keycloak;

    @Autowired
    public KeycloakUtil(MetadataMapper metadataMapper, Keycloak keycloak) {
        this.metadataMapper = metadataMapper;
        this.keycloak = keycloak;
    }

    public void createUser(UserCreateDto data) throws AuthServiceException {
        final UserRepresentation user = metadataMapper.userCreateDtoToUserRepresentation(data);
        try (Response response = keycloak.realm("dbrepo")
                .users()
                .create(user)) {
            if (response.getStatus() != 200) {
                log.error("Failed to delete user: unexpected response status: {}", response.getStatus());
                throw new AuthServiceException("Unexpected response status: " + response.getStatus());
            }
        }
        log.info("Created user at auth service");
    }

    public boolean existsByUsername(String username) {
        return keycloak.realm("dbrepo")
                .users()
                .search(username)
                .isEmpty();
    }
}
