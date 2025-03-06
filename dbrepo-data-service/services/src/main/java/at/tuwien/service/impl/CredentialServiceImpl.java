package at.tuwien.service.impl;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.service.CredentialService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
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
