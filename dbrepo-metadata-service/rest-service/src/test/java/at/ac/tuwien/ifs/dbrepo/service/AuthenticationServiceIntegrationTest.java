package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.KeycloakUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthenticationServiceIntegrationTest extends BaseTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private KeycloakGateway keycloakGateway;

    @Autowired
    private KeycloakUtils keycloakUtils;

    @Container
    private static final KeycloakContainer keycloakContainer = new KeycloakContainer(KEYCLOAK_IMAGE)
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withRealmImportFile("./init/dbrepo-realm.json")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false");

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        final String authServiceEndpoint = "http://localhost:" + keycloakContainer.getMappedPort(8080);
        log.trace("set auth endpoint: {}", authServiceEndpoint);
        registry.add("dbrepo.endpoints.authService", () -> authServiceEndpoint);
    }

    @Test
    public void delete_succeeds() throws EmailExistsException, UserExistsException, UserNotFoundException,
            AuthServiceException, AuthServiceConnectionException, CredentialsInvalidException {

        /* mock */
        keycloakUtils.deleteUser(USER_1_USERNAME);
        keycloakUtils.createUser(USER_1_ID, USER_1_KEYCLOAK_SIGNUP_REQUEST);
        final User request = User.builder()
                .keycloakId(UUID.fromString(keycloakGateway.findByUsername(USER_1_USERNAME).getId()))
                .username(USER_1_USERNAME)
                .build();

        /* test */
        authenticationService.delete(request);
    }

}
