package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.utils.KeycloakUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
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

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AuthenticationServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private KeycloakGateway keycloakGateway;

    @Autowired
    private KeycloakUtils keycloakUtils;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Container
    private static KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:24.0")
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withRealmImportFile("./init/dbrepo-realm.json")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false");

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.endpoints.authService", () -> "http://localhost:" + keycloakContainer.getMappedPort(8080));
    }

    @Test
    public void delete_succeeds() throws EmailExistsException, UserExistsException, UserNotFoundException,
            AuthServiceException, AuthServiceConnectionException, CredentialsInvalidException {

        /* mock */
        try {
            keycloakGateway.deleteUser(keycloakGateway.findByUsername(USER_1_USERNAME).getId());
        } catch (Exception e) {
            /* ignore */
        }
        keycloakUtils.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        final User request = User.builder()
                .id(keycloakGateway.findByUsername(USER_1_USERNAME).getId())
                .username(USER_1_USERNAME)
                .build();

        /* test */
        authenticationService.delete(request);
    }

}
