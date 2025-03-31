package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.UserRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.KeycloakUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log4j2
@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class UserServiceIntegrationTest extends BaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private KeycloakUtils keycloakUtils;

    @BeforeEach
    public void beforeEach() {
        /* keycloak */
        userRepository.deleteAll();
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
    public void create_succeeds() throws UserNotFoundException, AuthServiceException {

        /* test */
        final User response = userService.create(USER_1_CREATE_USER_DTO);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_KEYCLOAK_ID, response.getKeycloakId());
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_THEME, response.getTheme());
        assertNotNull(response.getMariadbPassword());
        assertEquals(USER_1_LANGUAGE, response.getLanguage());
        assertEquals(USER_1_FIRSTNAME, response.getFirstname());
        assertEquals(USER_1_LASTNAME, response.getLastname());
        assertEquals(USER_1_IS_INTERNAL, response.getIsInternal());
    }

}
