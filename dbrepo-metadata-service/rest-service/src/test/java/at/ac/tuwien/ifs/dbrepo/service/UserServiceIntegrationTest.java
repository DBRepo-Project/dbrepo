package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.utils.KeycloakUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServiceIntegrationTest extends BaseTest {

    @Autowired
    private UserService userService;

    @Autowired
    private MetadataMapper metadataMapper;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private KeycloakUtils keycloakUtils;

    @MockitoBean
    private KeycloakGateway keycloakGateway;

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
    public void findByUsername_succeeds() throws UserNotFoundException, NotAllowedException {
        final UserRepresentation mockUser = metadataMapper.userDtoToUserRepresentation(USER_1_DTO);
        mockUser.setRealmRoles(Arrays.asList(DEFAULT_RESEARCHER_ROLES));

        /* mock */
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(mockUser);

        /* test */
        final UserDto response = userService.findByUsername(USER_1_USERNAME);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
    }
}
