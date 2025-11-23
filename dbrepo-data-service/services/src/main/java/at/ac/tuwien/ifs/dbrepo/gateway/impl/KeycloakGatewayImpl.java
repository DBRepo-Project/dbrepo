package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.KeycloakConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import jakarta.ws.rs.NotAuthorizedException;
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

    @Autowired
    public KeycloakGatewayImpl(KeycloakConfig keycloakConfig, MetadataMapper metadataMapper) {
        this.keycloakConfig = keycloakConfig;
        this.metadataMapper = metadataMapper;
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
