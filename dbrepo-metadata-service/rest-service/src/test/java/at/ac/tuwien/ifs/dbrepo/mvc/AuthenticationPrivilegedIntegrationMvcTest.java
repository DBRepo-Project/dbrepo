package at.ac.tuwien.ifs.dbrepo.mvc;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.CredentialsInvalidException;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.repository.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.repository.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.repository.LicenseRepository;
import at.ac.tuwien.ifs.dbrepo.repository.UserRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.KeycloakUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@Testcontainers
@SpringBootTest
public class AuthenticationPrivilegedIntegrationMvcTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KeycloakUtils keycloakUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
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
        registry.add("dbrepo.endpoints.authService", () -> "http://localhost:" + keycloakContainer.getMappedPort(8080));
    }

    @BeforeEach
    public void beforeEach() throws AuthServiceException, AuthServiceConnectionException, CredentialsInvalidException {
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4, USER_LOCAL));
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        /* keycloak */
        keycloakUtils.deleteUser(USER_1_USERNAME);
        keycloakUtils.deleteUser(USER_LOCAL_ADMIN_USERNAME);
    }

    @Test
    public void findById_database_basicUser_succeeds() throws Exception {

        /* mock */
        keycloakUtils.createUser(USER_1_ID, USER_1_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID).with(httpBasic(USER_1_USERNAME, USER_1_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void findById_database_basicAdmin_succeeds() throws Exception {

        /* pre condition */
        keycloakUtils.createUser(USER_LOCAL_ADMIN_ID, USER_LOCAL_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID).with(httpBasic(USER_LOCAL_ADMIN_USERNAME, USER_LOCAL_ADMIN_PASSWORD)))
                .andDo(print())
                .andExpect(header().string("X-Host", CONTAINER_1_HOST))
                .andExpect(header().string("X-Port", "" + CONTAINER_1_PORT))
                .andExpect(header().string("X-Username", CONTAINER_1_PRIVILEGED_USERNAME))
                .andExpect(header().string("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD))
                .andExpect(header().string("X-Jdbc-Method", IMAGE_1_JDBC_METHOD))
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Username X-Password X-Jdbc-Method X-Host X-Port"))
                .andExpect(status().isOk());
    }

    @Test
    public void findById_database_bearerAdmin_succeeds() throws Exception {

        /* pre condition */
        keycloakUtils.createUser(USER_LOCAL_ADMIN_ID, USER_LOCAL_KEYCLOAK_SIGNUP_REQUEST);
        final TokenDto jwt = keycloakGateway.obtainUserToken(USER_LOCAL_ADMIN_USERNAME, USER_LOCAL_ADMIN_PASSWORD);

        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID).header("Authorization", "Bearer " + jwt.getAccessToken()))
                .andDo(print())
                .andExpect(header().string("X-Host", CONTAINER_1_HOST))
                .andExpect(header().string("X-Port", "" + CONTAINER_1_PORT))
                .andExpect(header().string("X-Username", CONTAINER_1_PRIVILEGED_USERNAME))
                .andExpect(header().string("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD))
                .andExpect(header().string("X-Jdbc-Method", IMAGE_1_JDBC_METHOD))
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Username X-Password X-Jdbc-Method X-Host X-Port"))
                .andExpect(status().isOk());
    }

    @Test
    public void findById_table_bearerAdmin_succeeds() throws Exception {

        /* pre condition */
        keycloakUtils.createUser(USER_LOCAL_ADMIN_ID, USER_LOCAL_KEYCLOAK_SIGNUP_REQUEST);
        final TokenDto jwt = keycloakGateway.obtainUserToken(USER_LOCAL_ADMIN_USERNAME, USER_LOCAL_ADMIN_PASSWORD);


        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID + "/table/" + TABLE_1_ID).header("Authorization", "Bearer " + jwt.getAccessToken()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void findById_table_basicUser_succeeds() throws Exception {

        /* mock */
        keycloakUtils.createUser(USER_1_ID, USER_1_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID + "/table/" + TABLE_1_ID).with(httpBasic(USER_1_USERNAME, USER_1_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void findById_table_basicAdmin_succeeds() throws Exception {

        /* mock */
        keycloakUtils.createUser(USER_LOCAL_ADMIN_ID, USER_LOCAL_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID + "/table/" + TABLE_1_ID).with(httpBasic(USER_LOCAL_ADMIN_USERNAME, USER_LOCAL_ADMIN_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    public void findById_view_basicUser_succeeds() throws Exception {

        /* mock */
        keycloakUtils.createUser(USER_1_ID, USER_1_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID + "/view/" + VIEW_1_ID).with(httpBasic(USER_1_USERNAME, USER_1_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void findById_container_basicUser_succeeds() throws Exception {

        /* mock */
        keycloakUtils.createUser(USER_1_ID, USER_1_KEYCLOAK_SIGNUP_REQUEST);

        /* test */
        this.mockMvc.perform(get("/api/v1/container/" + CONTAINER_1_ID).with(httpBasic(USER_1_USERNAME, USER_1_PASSWORD)))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
