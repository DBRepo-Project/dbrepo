package at.tuwien.gateway.impl;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.MetadataMapper;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j2;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class KeycloakGatewayImpl implements KeycloakGateway {

    private final Keycloak keycloak;
    private final KeycloakConfig keycloakConfig;
    private final MetadataMapper metadataMapper;

    public KeycloakGatewayImpl(Keycloak keycloak, KeycloakConfig keycloakConfig, MetadataMapper metadataMapper) {
        this.keycloak = keycloak;
        this.keycloakConfig = keycloakConfig;
        this.metadataMapper = metadataMapper;
    }

    @Override
    public TokenDto obtainUserToken(String username, String password) {
        try (Keycloak userKeycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getKeycloakEndpoint())
                .realm(keycloakConfig.getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(keycloakConfig.getKeycloakClient())
                .clientSecret(keycloakConfig.getKeycloakClientSecret())
                .username(username)
                .password(password)
                .build()) {
            return metadataMapper.accessTokenResponseToTokenDto(userKeycloak.tokenManager()
                    .getAccessToken());
        }
    }

    @Override
    public UserRepresentation findByUsername(String username) throws UserNotFoundException {
        final List<UserRepresentation> users = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .search(username);
        if (users.isEmpty()) {
            log.error("Failed to find user with username {}", username);
            throw new UserNotFoundException("Failed to find user");
        }
        return users.get(0);
    }

    @Override
    public void deleteUser(UUID id) throws AuthServiceException {
        try (Response response = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .delete(String.valueOf(id))) {
            if (response.getStatus() != 200) {
                log.error("Failed to delete user: unexpected response status: {}", response.getStatus());
                throw new AuthServiceException("Unexpected response status: " + response.getStatus());
            }
        }
        log.info("Deleted user {} at auth service", id);
    }

    @Override
    public void updateUserCredentials(UUID id, UserPasswordDto data) {
        final CredentialRepresentation credential = new CredentialRepresentation();
        credential.setCredentialData(data.getPassword());
        credential.setType(CredentialRepresentation.PASSWORD);
        keycloak.realm(keycloakConfig.getRealm())
                .users()
                .get(String.valueOf(id))
                .resetPassword(credential);
        log.info("Updated user {} password at auth service", id);
    }

    @Override
    public void updateUser(UUID id, UserUpdateDto data) throws AuthServiceException, UserNotFoundException {
        final UserResource resource = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .get(String.valueOf(id));
        UserRepresentation user;
        try {
            user = resource.toRepresentation();
        } catch (NotFoundException e) {
            log.error("Failed to update user: not found: {}", e.getMessage());
            throw new UserNotFoundException("Failed to update user: not found", e);
        }
        user.setFirstName(data.getFirstname());
        user.setLastName(data.getLastname());
        user.singleAttribute("THEME", data.getTheme());
        user.singleAttribute("ORCID", data.getOrcid());
        user.singleAttribute("LANGUAGE", data.getLanguage());
        user.singleAttribute("AFFILIATION", data.getAffiliation());
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
