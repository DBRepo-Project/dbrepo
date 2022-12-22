package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.DatabaseidxRepository;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.impl.HibernateConnector;
import at.tuwien.service.impl.MariaDbServiceImpl;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class DatabaseServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private DatabaseidxRepository databaseidxRepository;

    @MockBean
    private Channel channel;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MariaDbServiceImpl databaseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Container CONTAINER_SEARCH = Container.builder()
            .name(SEARCH_NAME)
            .internalName(SEARCH_NAME)
            .build();

    @BeforeEach
    public void beforeEach() throws InterruptedException {
        afterEach();
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
        /* create fda-userdb-u01 */
        final CreateContainerResponse response1 = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb"))
                .withName(CONTAINER_1_NAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withEnv("MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb")
                .exec();
        CONTAINER_1.setHash(response1.getId());
        /* create fda-userdb-u02 */
        final CreateContainerResponse response2 = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb"))
                .withName(CONTAINER_2_NAME)
                .withIpv4Address(CONTAINER_2_IP)
                .withHostName(CONTAINER_2_INTERNALNAME)
                .withEnv("MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb")
                .exec();
        CONTAINER_2.setHash(response2.getId());
        /* start containers */
        startContainer(CONTAINER_1);
        startContainer(CONTAINER_2);
    }

    @AfterEach
    public void afterEach() {
        /* stop containers and remove them */
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    log.info("Delete container {}", container.getNames()[0]);
                    try {
                        dockerClient.stopContainerCmd(container.getId()).exec();
                    } catch (NotModifiedException e) {
                        // ignore
                    }
                    dockerClient.removeContainerCmd(container.getId()).exec();
                });

        /* remove networks */
        dockerClient.listNetworksCmd()
                .exec()
                .stream()
                .filter(n -> n.getName().startsWith("fda"))
                .forEach(network -> {
                    log.info("Delete network {}", network.getName());
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                });
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException, AmqpException,
            ContainerNotFoundException, ContainerConnectionException, DatabaseMalformedException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseidxRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        databaseService.create(CONTAINER_1_ID, DATABASE_1_CREATE, principal);
    }

    @Test
    public void create_multiple_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException, AmqpException,
            ContainerNotFoundException, ContainerConnectionException, DatabaseMalformedException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseidxRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        final Database database1 = databaseService.create(CONTAINER_1_ID, DATABASE_1_CREATE, principal);
        assertEquals(DATABASE_1_NAME, database1.getName());
        assertEquals(1, userRepository.findAll().size());
        final Database database2 = databaseService.create(CONTAINER_2_ID, DATABASE_2_CREATE, principal);
        assertEquals(DATABASE_2_NAME, database2.getName());
        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    public void create_databaseAfterAnother_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException, AmqpException,
            ContainerNotFoundException, ContainerConnectionException, DatabaseMalformedException, InterruptedException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(databaseidxRepository.save(any(Database.class)))
                .thenReturn(DATABASE_3);
        when(databaseidxRepository.save(any(Database.class)))
                .thenReturn(DATABASE_4);
        createContainer(CONTAINER_3, "MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb");
        containerRepository.save(CONTAINER_3);
        createContainer(CONTAINER_4, "MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb");
        containerRepository.save(CONTAINER_4);
        startContainer(CONTAINER_3);
        startContainer(CONTAINER_4);


        /* test */
        final Database database4 = databaseService.create(CONTAINER_4_ID, DATABASE_4_CREATE, principal);
        assertEquals(DATABASE_4_NAME, database4.getName());
        final Database database3 = databaseService.create(CONTAINER_3_ID, DATABASE_3_CREATE, principal);
        assertEquals(DATABASE_3_NAME, database3.getName());
    }

}
