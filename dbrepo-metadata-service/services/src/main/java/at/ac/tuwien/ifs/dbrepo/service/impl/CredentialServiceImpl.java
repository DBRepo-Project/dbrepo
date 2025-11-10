package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.cache.TokenCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.CacheConfig;
import at.ac.tuwien.ifs.dbrepo.config.KeycloakConfig;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Token;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.service.CredentialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class CredentialServiceImpl implements CredentialService {

    private final CacheConfig cacheConfig;
    private final KeycloakConfig keycloakConfig;
    private final KeycloakGateway keycloakGateway;
    private final TokenCacheRepository tokenRepository;

    @Autowired
    public CredentialServiceImpl(CacheConfig cacheConfig, KeycloakConfig keycloakConfig,
                                 KeycloakGateway keycloakGateway, TokenCacheRepository tokenRepository) {
        this.cacheConfig = cacheConfig;
        this.keycloakConfig = keycloakConfig;
        this.keycloakGateway = keycloakGateway;
        this.tokenRepository = tokenRepository;
    }

    @Override
    public String getAdminToken(String username, String password) {
        return getToken(username, password, "master", "admin-cli", null);
    }

    private String getToken(String username, String password, String realm, String clientId, String clientSecret) {
        final Optional<Token> optional = tokenRepository.findById(username);
        if (optional.isPresent()) {
            log.trace("cache hit for token: {}", username);
            return optional.get()
                    .getToken();
        }
        log.trace("cache miss for token: {}", username);
        final Token token = Token.builder()
                .username(username)
                .token(keycloakGateway.getUserToken(username, password, realm, clientId, clientSecret)
                        .getAccessToken())
                .exp(cacheConfig.getTtl())
                .build();
        return tokenRepository.save(token)
                .getToken();
    }

    @Override
    public String getUserToken(String username, String password) {
        return getToken(username, password, keycloakConfig.getKeycloakRealm(), keycloakConfig.getKeycloakClient(),
                keycloakConfig.getKeycloakClientSecret());
    }

}
