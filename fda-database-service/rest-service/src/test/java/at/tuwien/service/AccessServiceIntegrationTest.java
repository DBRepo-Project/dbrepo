package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class AccessServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private Channel channel;

    @Autowired
    private DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private AccessService accessService;

    @Autowired
    private UserRepository userRepository;

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
        /* create container */
        final String bind = new File(
                "./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse response1 = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb").withBinds(Bind.parse(bind)))
                .withName(CONTAINER_1_NAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withVolumes()
                .withEnv("MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb")
                .exec();
        CONTAINER_1.setHash(response1.getId());
        startContainer(CONTAINER_1);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setCreator(USER_1);
        databaseRepository.save(DATABASE_1);
    }

    @AfterEach
    public void afterEach() {
        /* stop containers and remove them */
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
    public void create_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(DATABASE_1_READ_ACCESS_TYPE_DTO)
                .username(USER_2_USERNAME)
                .build();

        /* test */
        accessService.create(CONTAINER_1_ID, DATABASE_1_ID, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_READ_ACCESS_TYPE, response.get(0).getType());
        assertEquals(DATABASE_1_ID, response.get(0).getHdbid());
    }

    @Test
    public void create_multiple_fails() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(DATABASE_1_READ_ACCESS_TYPE_DTO)
                .username(USER_2_USERNAME)
                .build();

        /* test */
        accessService.create(CONTAINER_1_ID, DATABASE_1_ID, request);
        assertThrows(NotAllowedException.class, () -> {
            accessService.create(CONTAINER_1_ID, DATABASE_1_ID, request);
        });
    }

    @Test
    public void create_creator_fails() {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(DATABASE_1_READ_ACCESS_TYPE_DTO)
                .username(USER_1_USERNAME)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessService.create(CONTAINER_1_ID, DATABASE_1_ID, request);
        });
    }

    @Test
    public void update_same_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(DATABASE_1_READ_ACCESS_TYPE_DTO)
                .build();

        /* test */
        accessService.update(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_READ_ACCESS_TYPE, response.get(0).getType());
        assertEquals(DATABASE_1_ID, response.get(0).getHdbid());
        assertEquals(USER_2_ID, response.get(0).getHuserid());
    }

    @Test
    public void update_writeOwn_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(DATABASE_2_WRITE_OWN_ACCESS_TYPE_DTO)
                .build();

        /* test */
        accessService.update(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(DATABASE_2_WRITE_OWN_ACCESS_TYPE, response.get(0).getType());
        assertEquals(DATABASE_1_ID, response.get(0).getHdbid());
        assertEquals(USER_2_ID, response.get(0).getHuserid());
    }

    @Test
    public void update_writeAll_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(DATABASE_3_WRITE_ALL_ACCESS_TYPE_DTO)
                .build();

        /* test */
        accessService.update(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(DATABASE_3_WRITE_ALL_ACCESS_TYPE, response.get(0).getType());
        assertEquals(DATABASE_1_ID, response.get(0).getHdbid());
        assertEquals(USER_2_ID, response.get(0).getHuserid());
    }

    @Test
    public void update_userNotFound_fails() {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(DATABASE_1_READ_ACCESS_TYPE_DTO)
                .build();

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessService.update(CONTAINER_1_ID, DATABASE_1_ID, "l33tsp34k", request);
        });
    }

    @Test
    public void update_databaseNotFound_fails() {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(DATABASE_1_READ_ACCESS_TYPE_DTO)
                .build();

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessService.update(CONTAINER_2_ID, DATABASE_2_ID, USER_2_USERNAME, request);
        });
    }

    @Test
    public void delete_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException {

        /* test */
        accessService.delete(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME);
    }

    @Test
    public void delete_isOwner_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessService.delete(CONTAINER_1_ID, DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void delete_notExists_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessService.delete(CONTAINER_1_ID, DATABASE_1_ID, "l33tsp34k");
        });
    }

}
