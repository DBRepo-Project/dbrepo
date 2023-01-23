package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.AccessEndpoint;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.Arrays;

import static at.tuwien.config.DockerConfig.dockerClient;
import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AccessEndpointIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private Channel channel;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    private AccessEndpoint accessEndpoint;

    private final static String BIND = new File("./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        /* create networks */
        dockerClient.createNetworkCmd()
                .withName("fda-userdb")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.28.0.0/16")))
                .withEnableIpv6(false)
                .exec();
        dockerClient.createNetworkCmd()
                .withName("fda-public")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.29.0.0/16")))
                .withEnableIpv6(false)
                .exec();
    }

    @AfterAll
    public static void afterAll() {
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    log.info("Delete container {}", Arrays.asList(container.getNames()));
                    try {
                        dockerClient.stopContainerCmd(container.getId()).exec();
                    } catch (NotModifiedException e) {
                        // ignore
                    }
                    dockerClient.removeContainerCmd(container.getId()).exec();
                });
        dockerClient.listNetworksCmd()
                .exec()
                .stream()
                .filter(n -> n.getName().startsWith("fda"))
                .forEach(network -> {
                    log.info("Delete network {}", network.getName());
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                });
    }

    @AfterEach
    public void afterEach() {
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    log.info("Delete container {}", Arrays.asList(container.getNames()));
                    try {
                        dockerClient.stopContainerCmd(container.getId()).exec();
                    } catch (NotModifiedException e) {
                        // ignore
                    }
                    dockerClient.removeContainerCmd(container.getId()).exec();
                });
    }

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, InterruptedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(AccessTypeDto.READ)
                .username(USER_2_USERNAME)
                .build();

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        final ResponseEntity<?> response = accessEndpoint.create(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void update_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, AccessDeniedException, InterruptedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(AccessTypeDto.READ)
                .build();

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        databaseAccessRepository.save(DATABASE_1_READ_ACCESS);

        /* test */
        final ResponseEntity<?> response = accessEndpoint.update(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void revoke_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, AccessDeniedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        databaseAccessRepository.save(DATABASE_1_READ_ACCESS);

        /* test */
        final ResponseEntity<?> response = accessEndpoint.revoke(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
