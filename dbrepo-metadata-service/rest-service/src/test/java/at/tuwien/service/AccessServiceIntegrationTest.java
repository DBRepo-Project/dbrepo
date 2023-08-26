package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class AccessServiceIntegrationTest extends BaseUnitTest {

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

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
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
                Arguments.arguments("same access", DATABASE_1_ID, AccessTypeDto.READ, AccessType.READ,
                        USER_2_USERNAME, USER_2_ID),
                Arguments.arguments("write own access", DATABASE_1_ID, AccessTypeDto.WRITE_OWN,
                        AccessType.WRITE_OWN, USER_2_USERNAME, USER_2_ID),
                Arguments.arguments("write all access", DATABASE_1_ID, AccessTypeDto.WRITE_ALL,
                        AccessType.WRITE_ALL, USER_2_USERNAME, USER_2_ID)
        );
    }

    public static Stream<Arguments> update_fails_parameters() {
        return Stream.of(
                Arguments.arguments("user not found", UserNotFoundException.class, DATABASE_1_ID,
                        AccessTypeDto.READ, "l33tsp34k"),
                Arguments.arguments("database not found", DatabaseNotFoundException.class, DATABASE_2_ID,
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
            accessService.create(DATABASE_1_ID, request);
        });
    }

    @ParameterizedTest
    @MethodSource("create_succeeds_parameters")
    protected <T extends Throwable> void create_succeeds(String test, AccessTypeDto accessTypeDto, AccessType access,
                                                         String username, UUID userId) throws UserNotFoundException,
            NotAllowedException, QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException,
            KeycloakRemoteException, AccessDeniedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(accessTypeDto)
                .username(username)
                .build();

        /* test */
        accessService.create(DATABASE_1_ID, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(access, response.get(0).getType());
        assertEquals(DATABASE_1_ID, response.get(0).getHdbid());
        assertEquals(userId, response.get(0).getHuserid());
    }

    @ParameterizedTest
    @MethodSource("update_succeeds_parameters")
    protected void update_succeeds(String test, Long databaseId, AccessTypeDto accessTypeDto, AccessType access,
                                   UUID userId) throws UserNotFoundException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, NotAllowedException, KeycloakRemoteException,
            AccessDeniedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* mock */
        databaseAccessRepository.save(DATABASE_1_USER_2_READ_ACCESS);

        /* test */
        accessService.update(databaseId, userId, request);
        final List<DatabaseAccess> response = databaseAccessRepository.findAll();
        assertEquals(1, response.size());
        assertEquals(access, response.get(0).getType());
        assertEquals(databaseId, response.get(0).getHdbid());
    }

    @ParameterizedTest
    @MethodSource("update_fails_parameters")
    protected <T extends Throwable> void update_fails(String name, Class<T> expectedException,
                                                      Long databaseId, AccessTypeDto accessTypeDto, UUID userId) {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* test */
        assertThrows(expectedException, () -> {
            accessService.update(databaseId, userId, request);
        });
    }

    @ParameterizedTest
    @MethodSource("delete_fails_parameters")
    protected <T extends Throwable> void delete_fails(String name, Class<T> expectedException, UUID userId) {

        /* test */
        assertThrows(expectedException, () -> {
            accessService.delete(DATABASE_1_ID, userId);
        });
    }

    @ParameterizedTest
    @MethodSource("delete_succeeds_parameters")
    protected <T extends Throwable> void delete_succeeds(String name, UUID userId) throws UserNotFoundException,
            NotAllowedException, QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException,
            KeycloakRemoteException, AccessDeniedException {

        /* test */
        accessService.delete(DATABASE_1_ID, userId);
    }

}
