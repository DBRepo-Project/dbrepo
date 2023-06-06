package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    private AccessService accessService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    private final static String BIND_WEATHER = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

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
        DockerConfig.createAllNetworks();
        /* metadata database */
        realmRepository.save(REALM_DBREPO);
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1_SIMPLE);
        userRepository.save(USER_2_SIMPLE);
        userRepository.save(USER_3_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    public static Stream<Arguments> create_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("general", AccessTypeDto.READ, AccessType.READ, USER_3_USERNAME, USER_3_ID)
        );
    }

    public static Stream<Arguments> create_fails_parameters() {
        return Stream.of(
                Arguments.arguments("general", NotAllowedException.class, AccessTypeDto.READ, USER_2_USERNAME)
        );
    }

    public static Stream<Arguments> update_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("same access", CONTAINER_1_ID, DATABASE_1_ID, AccessTypeDto.READ, AccessType.READ,
                        USER_2_USERNAME, USER_2_ID),
                Arguments.arguments("write own access", CONTAINER_1_ID, DATABASE_1_ID, AccessTypeDto.WRITE_OWN,
                        AccessType.WRITE_OWN, USER_2_USERNAME, USER_2_ID),
                Arguments.arguments("write all access", CONTAINER_1_ID, DATABASE_1_ID, AccessTypeDto.WRITE_ALL,
                        AccessType.WRITE_ALL, USER_2_USERNAME, USER_2_ID)
        );
    }

    public static Stream<Arguments> update_fails_parameters() {
        return Stream.of(
                Arguments.arguments("user not found", UserNotFoundException.class, CONTAINER_1_ID, DATABASE_1_ID,
                        AccessTypeDto.READ, "l33tsp34k"),
                Arguments.arguments("database not found", DatabaseNotFoundException.class, CONTAINER_2_ID, DATABASE_2_ID,
                        AccessTypeDto.READ, USER_2_USERNAME)
        );
    }

    public static Stream<Arguments> delete_fails_parameters() {
        return Stream.of(
                Arguments.arguments("user not found", UserNotFoundException.class, "l33tsp34k"),
                Arguments.arguments("is owner", NotAllowedException.class, USER_1_USERNAME)
        );
    }

    public static Stream<Arguments> delete_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("general", USER_2_USERNAME)
        );
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    @ParameterizedTest
    @MethodSource("create_fails_parameters")
    protected <T extends Throwable> void create_fails(String test, Class<T> expectedException,
                                                      AccessTypeDto accessTypeDto, String username) {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(accessTypeDto)
                .username(username)
                .build();

        /* mock */
        databaseAccessRepository.save(DATABASE_1_USER_2_READ_ACCESS);

        /* test */
        assertThrows(expectedException, () -> {
            accessService.create(CONTAINER_1_ID, DATABASE_1_ID, request);
        });
    }

    @ParameterizedTest
    @MethodSource("create_succeeds_parameters")
    protected <T extends Throwable> void create_succeeds(String test, AccessTypeDto accessTypeDto, AccessType access,
                                                         String username, UUID userId)
            throws UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseNotFoundException,
            DatabaseMalformedException, InterruptedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(accessTypeDto)
                .username(username)
                .build();

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);

        /* test */
        accessService.create(CONTAINER_1_ID, DATABASE_1_ID, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(access, response.get(0).getType());
        assertEquals(DATABASE_1_ID, response.get(0).getHdbid());
        assertEquals(userId, response.get(0).getHuserid());
    }

    @ParameterizedTest
    @MethodSource("update_succeeds_parameters")
    protected void update_succeeds(String test, Long containerId, Long databaseId, AccessTypeDto accessTypeDto, AccessType access,
                                   String username) throws UserNotFoundException, NotAllowedException,
            QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException, AccessDeniedException, InterruptedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* mock */
        databaseAccessRepository.save(DATABASE_1_USER_2_READ_ACCESS);
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);

        /* test */
        accessService.update(containerId, databaseId, username, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(access, response.get(0).getType());
        assertEquals(databaseId, response.get(0).getHdbid());
    }

    @ParameterizedTest
    @MethodSource("update_fails_parameters")
    protected <T extends Throwable> void update_fails(String name, Class<T> expectedException, Long containerId,
                                                      Long databaseId, AccessTypeDto accessTypeDto,
                                                      String username) {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* test */
        assertThrows(expectedException, () -> {
            accessService.update(containerId, databaseId, username, request);
        });
    }

    @ParameterizedTest
    @MethodSource("delete_fails_parameters")
    protected <T extends Throwable> void delete_fails(String name, Class<T> expectedException, String username) {

        /* test */
        assertThrows(expectedException, () -> {
            accessService.delete(CONTAINER_1_ID, DATABASE_1_ID, username);
        });
    }

    @ParameterizedTest
    @MethodSource("delete_succeeds_parameters")
    protected <T extends Throwable> void delete_succeeds(String name, String username) throws InterruptedException,
            UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseNotFoundException,
            DatabaseMalformedException {

        /* mock */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);

        /* test */
        accessService.delete(CONTAINER_1_ID, DATABASE_1_ID, username);
    }

}
