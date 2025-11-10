package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.TokenCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Token;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class CredentialServiceUnitTest extends BaseTest {

    @MockitoBean
    private TokenCacheRepository tokenCacheRepository;

    @MockitoBean
    private KeycloakGateway keycloakGateway;

    @Autowired
    private CredentialService credentialService;

    @Test
    public void getAccessToken_cached_succeeds() {

        /* mock */
        when(tokenCacheRepository.findById(USER_LOCAL_ADMIN_USERNAME))
                .thenReturn(Optional.of(TOKEN_LOCAL_ADMIN_CACHE));

        /* test */
        final String token = credentialService.getAccessToken(USER_LOCAL_ADMIN_USERNAME, USER_LOCAL_ADMIN_PASSWORD);
        assertNotNull(token);
    }

    @Test
    public void getAccessToken_notCached_succeeds() {

        /* mock */
        when(tokenCacheRepository.findById(USER_LOCAL_ADMIN_USERNAME))
                .thenReturn(Optional.empty());
        when(keycloakGateway.obtainUserToken(USER_LOCAL_ADMIN_USERNAME, USER_LOCAL_ADMIN_PASSWORD))
                .thenReturn(TOKEN_DTO);
        when(tokenCacheRepository.save(any(Token.class)))
                .thenReturn(TOKEN_LOCAL_ADMIN_CACHE);

        /* test */
        final String token = credentialService.getAccessToken(USER_LOCAL_ADMIN_USERNAME, USER_LOCAL_ADMIN_PASSWORD);
        assertNotNull(token);
    }
}
