package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.License;
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
import org.springframework.transaction.annotation.Transactional;
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
    private AccessService accessService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setAccesses(List.of(DATABASE_1_USER_1_WRITE_ALL_ACCESS, DATABASE_1_USER_2_READ_ACCESS));
        databaseRepository.save(DATABASE_1);
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    public static Stream<Arguments> create_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("general", AccessTypeDto.READ, AccessType.READ, USER_3_ID)
        );
    }

    public static Stream<Arguments> create_fails_parameters() {
        return Stream.of(
                Arguments.arguments("general", NotAllowedException.class, AccessTypeDto.READ, USER_2_ID)
        );
    }

    public static Stream<Arguments> update_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("same access", DATABASE_1_ID, AccessTypeDto.READ, AccessType.WRITE_ALL,
                        USER_2_ID),
                Arguments.arguments("write all access", DATABASE_1_ID, AccessTypeDto.WRITE_ALL,
                        AccessType.WRITE_ALL, USER_2_ID)
        );
    }

    public static Stream<Arguments> update_fails_parameters() {
        return Stream.of(
                Arguments.arguments("user not found", UserNotFoundException.class, DATABASE_1_ID,
                        AccessTypeDto.READ, UUID.fromString("deadbeef-fc88-4abd-a289-455e34b0e80d"), null),
                Arguments.arguments("database not found", DatabaseNotFoundException.class, DATABASE_2_ID,
                        AccessTypeDto.READ, USER_1_ID)
        );
    }

    public static Stream<Arguments> delete_fails_parameters() {
        return Stream.of(
                Arguments.arguments("user not found", AccessDeniedException.class,
                        UUID.fromString("deadbeef-fc88-4abd-a289-455e34b0e80d"), null),
                Arguments.arguments("is owner", NotAllowedException.class, USER_1_ID)
        );
    }

    public static Stream<Arguments> delete_succeeds_parameters() {
        return Stream.of(
                Arguments.arguments("general", USER_2_ID)
        );
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    @Transactional
    @ParameterizedTest
    @MethodSource("create_fails_parameters")
    protected <T extends Throwable> void create_fails(String test, Class<T> expectedException,
                                                      AccessTypeDto accessTypeDto, UUID userId) {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* test */
        assertThrows(expectedException, () -> {
            accessService.create(DATABASE_1_ID, userId, request);
        });
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("create_succeeds_parameters")
    protected <T extends Throwable> void create_succeeds(String test, AccessTypeDto accessTypeDto, AccessType access,
                                                         UUID userId) throws UserNotFoundException,
            NotAllowedException, QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException,
            KeycloakRemoteException, AccessDeniedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* test */
        accessService.create(DATABASE_1_ID, userId, request);
        final List<DatabaseAccess> response = databaseRepository.findAll()
                .stream()
                .map(Database::getAccesses)
                .flatMap(List::stream)
                .distinct()
                .toList();
        assertEquals(3, response.size()); // 2+1
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("update_succeeds_parameters")
    protected void update_succeeds(String test, Long databaseId, AccessTypeDto accessTypeDto, AccessType access,
                                   UUID userId) throws UserNotFoundException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, NotAllowedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* test */
        accessService.update(databaseId, userId, request);
        final List<DatabaseAccess> response = databaseRepository.findAll()
                .stream()
                .map(Database::getAccesses)
                .flatMap(List::stream)
                .distinct()
                .toList();
        assertEquals(2, response.size());
        assertEquals(access, response.get(0).getType());
        assertEquals(databaseId, response.get(0).getDatabase().getId());
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("update_fails_parameters")
    protected <T extends Throwable> void update_fails(String name, Class<T> expectedException, Long databaseId,
                                                      AccessTypeDto accessTypeDto, UUID userId) {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(accessTypeDto)
                .build();

        /* test */
        assertThrows(expectedException, () -> {
            accessService.update(databaseId, userId, request);
        });
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("delete_fails_parameters")
    protected <T extends Throwable> void delete_fails(String name, Class<T> expectedException, UUID userId) {

        /* test */
        assertThrows(expectedException, () -> {
            accessService.delete(DATABASE_1_ID, userId);
        });
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("delete_succeeds_parameters")
    protected <T extends Throwable> void delete_succeeds(String name, UUID userId)
            throws UserNotFoundException, NotAllowedException, QueryMalformedException, DatabaseNotFoundException,
            DatabaseMalformedException, AccessDeniedException {

        /* test */
        accessService.delete(DATABASE_1_ID, userId);
    }

}
