package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.KeycloakConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ProcessingException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KeycloakGatewayImpl implements KeycloakGateway {

    private final KeycloakConfig keycloakConfig;
    private final MetadataMapper metadataMapper;
    private final Keycloak keycloak;

    @Autowired
    public KeycloakGatewayImpl(KeycloakConfig keycloakConfig, MetadataMapper metadataMapper, Keycloak keycloak) {
        this.keycloakConfig = keycloakConfig;
        this.metadataMapper = metadataMapper;
        this.keycloak = keycloak;
    }

    @Override
    public void validatePublisher(String username) throws NotAllowedException, RemoteUnavailableException {
        try {
            final var users = keycloak.realm(keycloakConfig.getKeycloakRealm()).users();
            final var matches = users.searchByUsername(username, true).stream()
                    .filter(user -> username.equals(user.getUsername()) && Boolean.TRUE.equals(user.isEnabled()))
                    .toList();
            if (matches.size() != 1 || users.get(matches.getFirst().getId()).roles().realmLevel().listEffective()
                    .stream().noneMatch(role -> "insert-table-data".equals(role.getName()))) {
                throw new NotAllowedException("Publisher account cannot insert table data");
            }
        } catch (WebApplicationException | ProcessingException e) {
            throw new RemoteUnavailableException("Failed to verify current publisher account permissions");
        }
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
    public TokenDto getUserToken(String username, String password) throws BadCredentialsException {
        try (Keycloak userKeycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getKeycloakEndpoint())
                .realm(keycloakConfig.getKeycloakRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(keycloakConfig.getKeycloakClient())
                .clientSecret(keycloakConfig.getKeycloakClientSecret())
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

}
