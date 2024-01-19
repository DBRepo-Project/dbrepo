package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
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
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class AuthenticationServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private KeycloakGateway keycloakGateway;

    @Container
    private static KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:21.0")
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withAdminUsername("fda")
            .withAdminPassword("fda")
            .withRealmImportFile("./dbrepo-realm.json")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false");

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("fda.keycloak.endpoint", () -> "http://localhost:" + keycloakContainer.getMappedPort(8080));
    }

    @Test
    public void delete_succeeds() throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException,
            UserEmailAlreadyExistsException, UserAlreadyExistsException {

        /* mock */
        try {
            keycloakGateway.deleteUser(keycloakGateway.findByUsername(USER_1_USERNAME).getId());
        } catch (Exception e) {
            /* ignore */
        }
        keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        authenticationService.delete(keycloakGateway.findByUsername(USER_1_USERNAME).getId());
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException,
            UserEmailAlreadyExistsException, UserAlreadyExistsException {

        /* mock */
        try {
            keycloakGateway.deleteUser(keycloakGateway.findByUsername(USER_1_USERNAME).getId());
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        authenticationService.create(USER_1_SIGNUP_REQUEST_DTO);
    }

}
