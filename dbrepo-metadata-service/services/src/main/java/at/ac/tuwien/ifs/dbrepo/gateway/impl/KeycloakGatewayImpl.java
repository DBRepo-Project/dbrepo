package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.KeycloakConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class KeycloakGatewayImpl implements KeycloakGateway {

    private final Keycloak keycloak;
    private final KeycloakConfig keycloakConfig;
    private final MetadataMapper metadataMapper;

    @Autowired
    public KeycloakGatewayImpl(Keycloak keycloak, KeycloakConfig keycloakConfig, MetadataMapper metadataMapper) {
        this.keycloak = keycloak;
        this.keycloakConfig = keycloakConfig;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public List<UserRepresentation> findAll() {
        log.debug("find all users in auth service");
        return keycloak.realm(keycloakConfig.getKeycloakRealm())
                .users()
                .list();
    }

    @Override
    public TokenDto getUserToken(String username, String password, String realm, String clientId, String clientSecret)
            throws BadCredentialsException {
        try (Keycloak userKeycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getKeycloakEndpoint())
                .realm(realm)
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .username(username)
                .password(password)
                .build()) {
            return metadataMapper.accessTokenResponseToTokenDto(userKeycloak.tokenManager()
                    .getAccessToken());
        } catch (NotAuthorizedException e) {
            log.error("Failed to obtain user token: {}", e.getMessage());
            throw new BadCredentialsException("Failed to obtain user token", e);
        }
    }

    @Override
    public UserRepresentation findByUsername(String username) throws UserNotFoundException, NotAllowedException {
        log.debug("find user in auth service by username: {}", username);
        final List<UserRepresentation> users = keycloak.realm(keycloakConfig.getKeycloakRealm())
                .users()
                .search(username);
        if (users.isEmpty()) {
            log.error("Failed to find user with username {}", username);
            throw new UserNotFoundException("Failed to find user");
        }
        return mapRoles(users.getFirst());
    }

    private UserRepresentation mapRoles(UserRepresentation user) {
        final List<String> roles = keycloak.realm(keycloakConfig.getKeycloakRealm())
                .users()
                .get(user.getId())
                .roles()
                .realmLevel()
                .listEffective()
                .stream()
                .map(RoleRepresentation::getName)
                .toList();
        log.trace("mapped found roles: {}", roles);
        user.setRealmRoles(roles);
        return user;
    }

    @Override
    public UserRepresentation findById(UUID id) throws UserNotFoundException, NotAllowedException {
        log.debug("find user in auth service by id: {}", id);
        final List<UserRepresentation> users = keycloak.realm(keycloakConfig.getKeycloakRealm())
                .users()
                .search(String.valueOf(id));
        if (users.isEmpty()) {
            log.error("Failed to find user with id {}", id);
            throw new UserNotFoundException("Failed to find user");
        }
        return mapRoles(users.getFirst());
    }

    @Override
    public void deleteUser(UUID id) throws UserNotFoundException {
        log.debug("delete user in auth service by id: {}", id);
        try (Response response = keycloak.realm(keycloakConfig.getKeycloakRealm())
                .users()
                .delete(String.valueOf(id))) {
            if (response.getStatus() == 404) {
                log.error("Failed to delete user: not found");
                throw new UserNotFoundException("Failed to delete user: not found");
            }
        }
        log.info("Deleted user {} at auth service", id);
    }

    @Override
    public void updateUser(UUID id, UserUpdateDto data) throws AuthServiceException, UserNotFoundException {
        log.debug("update user in auth service by id: {}", id);
        final UserResource resource = keycloak.realm(keycloakConfig.getKeycloakRealm())
                .users()
                .get(String.valueOf(id));
        final UserRepresentation user;
        try {
            user = resource.toRepresentation();
        } catch (NotFoundException e) {
            log.error("Failed to update user: not found: {}", e.getMessage());
            throw new UserNotFoundException("Failed to update user: not found", e);
        }
        user.setFirstName(data.getFirstname());
        user.setLastName(data.getLastname());
        user.singleAttribute("theme", data.getTheme());
        user.singleAttribute("orcid", data.getOrcid());
        user.singleAttribute("language", data.getLanguage());
        user.singleAttribute("affiliation", data.getAffiliation());
        log.trace("update user: {}", data);
        try {
            resource.update(user);
        } catch (ForbiddenException e) {
            log.error("Failed to update user: forbidden: {}", e.getMessage());
            throw new AuthServiceException("Failed to update user: forbidden", e);
        }
        log.info("Updated user {} attributes at auth service", id);
    }

}
