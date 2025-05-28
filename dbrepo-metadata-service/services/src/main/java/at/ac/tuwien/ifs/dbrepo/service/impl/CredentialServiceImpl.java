package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.service.CredentialService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CredentialServiceImpl implements CredentialService {

    private final KeycloakGateway keycloakGateway;
    private final Cache<String, TokenDto> tokenCache;

    @Autowired
    public CredentialServiceImpl(KeycloakGateway keycloakGateway, Cache<String, TokenDto> tokenCache) {
        this.tokenCache = tokenCache;
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    public TokenDto getAccessToken(String username, String password) {
        final TokenDto cacheAccessToken = tokenCache.getIfPresent(username);
        if (cacheAccessToken != null) {
            log.trace("found access token for user with username {} in cache", username);
            return cacheAccessToken;
        }
        log.debug("access token for user with username {} not it cache (anymore): request new", username);
        final TokenDto token = keycloakGateway.obtainUserToken(username, password);
        tokenCache.put(username, token);
        return token;
    }

    /**
     * Method for test cases to remove all caches.
     */
    public void invalidateAll() {
        tokenCache.invalidateAll();
    }

}
