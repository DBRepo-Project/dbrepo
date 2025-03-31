package at.tuwien.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.MetadataMapper;
import jakarta.ws.rs.NotAuthorizedException;
import lombok.extern.log4j.Log4j2;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Log4j2
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
    public TokenDto obtainUserToken(String username, String password) throws BadCredentialsException {
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
        } catch (NotAuthorizedException e) {
            log.error("Failed to obtain user token: {}", e.getMessage());
            throw new BadCredentialsException("Failed to obtain user token", e);
        }
    }

}
