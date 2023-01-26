package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.H2Utils;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.*;
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
import java.util.List;

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

    @Autowired
    private H2Utils h2Utils;

    private final static String BIND = new File("./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        DockerConfig.createAllNetworks();
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* metadata database */
        h2Utils.runScript("schema.sql");
        imageRepository.save(IMAGE_1);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        create_generic(DATABASE_1_READ_ACCESS_TYPE_DTO, DATABASE_1_READ_ACCESS_TYPE, USER_2_USERNAME, USER_2_ID);
    }

    @Test
    public void create_multiple_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        databaseAccessRepository.save(DATABASE_1_READ_ACCESS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_1_READ_ACCESS_TYPE_DTO, DATABASE_1_READ_ACCESS_TYPE, USER_2_USERNAME, USER_2_ID);
        });
    }

    @Test
    public void create_owner_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        databaseAccessRepository.save(DATABASE_1_OWNER_ACCESS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(DATABASE_1_READ_ACCESS_TYPE_DTO, DATABASE_1_READ_ACCESS_TYPE, USER_1_USERNAME, USER_1_ID);
        });
    }

    @Test
    public void update_same_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        databaseAccessRepository.save(DATABASE_1_READ_ACCESS);

        /* test */
        update_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1_READ_ACCESS_TYPE_DTO, DATABASE_1_READ_ACCESS_TYPE, USER_2_USERNAME, USER_2_ID);
    }

    @Test
    public void update_writeOwn_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        update_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_2_WRITE_OWN_ACCESS_TYPE_DTO, DATABASE_2_WRITE_OWN_ACCESS_TYPE, USER_2_USERNAME, USER_2_ID);
    }

    @Test
    public void update_writeAll_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        update_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_3_WRITE_ALL_ACCESS_TYPE_DTO, DATABASE_3_WRITE_ALL_ACCESS_TYPE, USER_2_USERNAME, USER_2_ID);
    }

    @Test
    public void update_userNotFound_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            update_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1_READ_ACCESS_TYPE_DTO, DATABASE_1_READ_ACCESS_TYPE, "l33tsp34k", null);
        });
    }

    @Test
    public void update_databaseNotFound_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_2, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        databaseRepository.save(DATABASE_1);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            update_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_1_READ_ACCESS_TYPE_DTO, DATABASE_1_READ_ACCESS_TYPE, USER_2_USERNAME, USER_2_ID);
        });
    }

    @Test
    public void delete_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        accessService.delete(CONTAINER_1_ID, DATABASE_1_ID, USER_2_USERNAME);
    }

    @Test
    public void delete_isOwner_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        userRepository.save(USER_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessService.delete(CONTAINER_1_ID, DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void delete_notExists_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(BIND, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            accessService.delete(CONTAINER_1_ID, DATABASE_1_ID, "l33tsp34k");
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void create_generic(AccessTypeDto accessTypeDto, AccessType access, String username, Long userId)
            throws UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseNotFoundException,
            DatabaseMalformedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(accessTypeDto)
                .username(username)
                .build();

        /* test */
        accessService.create(CONTAINER_1_ID, DATABASE_1_ID, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(access, response.get(0).getType());
        assertEquals(DATABASE_1_ID, response.get(0).getHdbid());
        assertEquals(userId, response.get(0).getHuserid());
    }

    protected void update_generic(Long containerId, Long databaseId, AccessTypeDto accessTypeDto, AccessType access,
                                  String username, Long userId) throws UserNotFoundException, NotAllowedException,
            QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* test */
        accessService.update(containerId, databaseId, username, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(access, response.get(0).getType());
        assertEquals(databaseId, response.get(0).getHdbid());
        assertEquals(userId, response.get(0).getHuserid());
    }

}
