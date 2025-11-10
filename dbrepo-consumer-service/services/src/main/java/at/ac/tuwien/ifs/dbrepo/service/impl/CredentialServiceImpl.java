package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.cache.TokenCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.CacheConfig;
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
    private final KeycloakGateway keycloakGateway;
    private final TokenCacheRepository tokenCacheRepository;

    @Autowired
    public CredentialServiceImpl(CacheConfig cacheConfig, KeycloakGateway keycloakGateway,
                                 TokenCacheRepository tokenCacheRepository) {
        this.cacheConfig = cacheConfig;
        this.keycloakGateway = keycloakGateway;
        this.tokenCacheRepository = tokenCacheRepository;
    }

    @Override
    public String getAccessToken(String username, String password) {
        final Optional<Token> optional = tokenCacheRepository.findById(username);
        if (optional.isPresent()) {
            log.trace("cache hit for token: {}", username);
            return optional.get()
                    .getToken();
        }
        log.trace("cache miss for token: {}", username);
        final Token token = Token.builder()
                .username(username)
                .token(keycloakGateway.obtainUserToken(username, password)
                        .getAccessToken())
                .exp(cacheConfig.getTtl())
                .build();
        return tokenCacheRepository.save(token)
                .getToken();
    }

}
