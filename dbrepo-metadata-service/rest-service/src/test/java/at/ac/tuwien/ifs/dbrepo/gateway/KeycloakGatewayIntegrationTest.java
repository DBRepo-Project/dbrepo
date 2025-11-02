package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.gateway.impl.KeycloakGatewayImpl;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.KeycloakUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
public class KeycloakGatewayIntegrationTest extends BaseTest {

    @Autowired
    private KeycloakGatewayImpl keycloakGateway;

    @Autowired
    private KeycloakUtils keycloakUtils;

    @BeforeEach
    public void beforeEach() {
        /* auth service */
        keycloakUtils.deleteUser(USER_1_USERNAME);
    }

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
    public void deleteUser_succeeds() throws UserNotFoundException {

        /* mock */
        keycloakUtils.createUser(USER_1_ID, USER_1_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        keycloakGateway.deleteUser(keycloakUtils.getUserId(USER_1_USERNAME));
    }

    @Test
    public void deleteUser_notFound_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void findByUsername_succeeds() throws UserNotFoundException, NotAllowedException {

        /* mock */
        keycloakUtils.createUser(USER_1_ID, USER_1_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        keycloakGateway.findByUsername(USER_1_USERNAME);
    }

    @Test
    public void findByUsername_notFound_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            keycloakGateway.findByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void updateUser_succeeds() throws UserNotFoundException, AuthServiceException {

        /* mock */
        keycloakUtils.createUser(USER_1_ID, USER_1_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        keycloakGateway.updateUser(keycloakUtils.getUserId(USER_1_USERNAME), USER_1_UPDATE_DTO);
        final UserRepresentation user = keycloakUtils.getUser(USER_1_USERNAME);
        assertNotNull(user.getId());
        assertEquals(USER_1_FIRSTNAME, user.getFirstName());
        assertEquals(USER_1_LASTNAME, user.getLastName());
        assertEquals(USER_1_THEME, user.firstAttribute("theme"));
        assertEquals(USER_1_ORCID_URL, user.firstAttribute("orcid"));
        assertEquals(USER_1_LANGUAGE, user.firstAttribute("language"));
        assertEquals(USER_1_AFFILIATION, user.firstAttribute("affiliation"));
    }

}
