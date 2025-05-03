package at.ac.tuwien.ifs.dbrepo.gateway.impl;

import at.ac.tuwien.ifs.dbrepo.config.KeycloakConfig;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
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

    private final DataMapper dataMapper;
    private final KeycloakConfig keycloakConfig;

    @Autowired
    public KeycloakGatewayImpl(DataMapper dataMapper, KeycloakConfig keycloakConfig) {
        this.dataMapper = dataMapper;
        this.keycloakConfig = keycloakConfig;
    }

    @Override
    public TokenDto obtainUserToken(String username, String password) throws BadCredentialsException {
        log.trace("obtain user token from endpoint={}, realm={}, clientId={}, username={}",
                keycloakConfig.getKeycloakEndpoint(), keycloakConfig.getRealm(), keycloakConfig.getKeycloakClient(),
                username);
        try (Keycloak userKeycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getKeycloakEndpoint())
                .realm(keycloakConfig.getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(keycloakConfig.getKeycloakClient())
                .clientSecret(keycloakConfig.getKeycloakClientSecret())
                .username(username)
                .password(password)
                .build()) {
            return dataMapper.accessTokenResponseToTokenDto(userKeycloak.tokenManager()
                    .getAccessToken());
        } catch (NotAuthorizedException e) {
            log.error("Failed to obtain user token: {}", e.getMessage());
            throw new BadCredentialsException("Failed to obtain user token", e);
        }
    }

}
