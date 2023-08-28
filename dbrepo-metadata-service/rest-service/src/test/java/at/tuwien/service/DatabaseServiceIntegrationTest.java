package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.*;
import at.tuwien.api.user.UserAttributesDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.impl.MariaDbServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class DatabaseServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @MockBean
    private KeycloakGateway keycloakGateway;

    @MockBean
    private UserService userService;

    @Autowired
    private DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private MariaDbServiceImpl databaseService;

    @Autowired
    private MariaDbConfig mariaDbConfig;

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.saveAll(List.of(DATABASE_1_SIMPLE, DATABASE_2_SIMPLE, DATABASE_3_SIMPLE));
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_2);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_3);
    }

    @Test
    public void find_succeeds() throws DatabaseNotFoundException {

        /* test */
        final Database response = databaseService.find(DATABASE_1_ID);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.find(9999L);
        });
    }

    @Test
    public void create_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        generic_create(DATABASE_1_CREATE, DATABASE_1);
    }

    @Test
    public void create_sameName_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        generic_create(DATABASE_1_CREATE, DATABASE_1);
        generic_create(DATABASE_1_CREATE, DATABASE_1);
    }

    @Test
    public void create_inSequence_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_2_INTERNALNAME);
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_3_INTERNALNAME);
        databaseRepository.deleteAll();
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_2_DTO)
                .thenReturn(DATABASE_3_DTO);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        generic_create(DATABASE_2_CREATE, DATABASE_2);
        generic_create(DATABASE_3_CREATE, DATABASE_3);
    }

    @Test
    public void create_outOfSequence_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_2_INTERNALNAME);
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_3_INTERNALNAME);
        databaseRepository.deleteAll();
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_3_DTO)
                .thenReturn(DATABASE_2_DTO);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        generic_create(DATABASE_3_CREATE, DATABASE_3);
        generic_create(DATABASE_2_CREATE, DATABASE_2);
    }

    @Test
    public void create_canLogin_succeeds() throws Exception {

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        databaseRepository.deleteAll();
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        final Database database = generic_create(DATABASE_1_CREATE, DATABASE_1);


        /* test */
        MariaDbConfig.getPrivileges(mariaDBContainer.getHost(), mariaDBContainer.getMappedPort(3306), database.getInternalName(), USER_1_USERNAME, USER_1_PASSWORD);
    }

    @Test
    public void updatePassword_canLogin_succeeds() throws Exception {
        final UserDto request = UserDto.builder()
                .id(USER_1_ID)
                .username(USER_1_USERNAME)
                .attributes(UserAttributesDto.builder()
                        .mariadbPassword(USER_2_DATABASE_PASSWORD)
                        .build())
                .build();

        /* mock */
        MariaDbConfig.dropDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createDatabase(CONTAINER_1, DATABASE_1_INTERNALNAME);
        databaseAccessRepository.save(DATABASE_1_USER_1_READ_ACCESS);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        databaseService.updatePassword(request);

        /* test */
        assertThrows(SQLInvalidAuthorizationSpecException.class, () -> {
            MariaDbConfig.getPrivileges(mariaDBContainer.getHost(), mariaDBContainer.getMappedPort(3306), DATABASE_1_INTERNALNAME, USER_1_USERNAME, USER_1_PASSWORD);
        });
        MariaDbConfig.getPrivileges(mariaDBContainer.getHost(), mariaDBContainer.getMappedPort(3306), DATABASE_1_INTERNALNAME, USER_1_USERNAME, USER_2_PASSWORD);
    }

    @Test
    public void create_queryStore_succeeds() throws Exception {

        /* test */
        generic_insert(QUERY_4_STATEMENT, 1L);
    }

    @Test
    public void create_queryStoreSameQueryHash_succeeds() throws Exception {

        /* test */
        generic_insert(QUERY_4_STATEMENT, 1L);
        generic_insert(QUERY_5_STATEMENT, 2L);
        generic_insert(QUERY_4_STATEMENT, 1L);
    }

    @Test
    public void create_systemProcedure_succeeds() throws Exception {

        /* test */
        generic_system_insert(CONTAINER_1_PRIVILEGED_USERNAME, CONTAINER_1_PRIVILEGED_PASSWORD);
    }

    @Test
    public void create_systemProcedure_fails() {

        /* test */
        assertThrows(SQLException.class, () -> {
            generic_system_insert("junit1", "junit1");
        });
    }

    @Test
    public void create_userProcedureRoot_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        generic_user_insert(CONTAINER_1_PRIVILEGED_USERNAME, CONTAINER_1_PRIVILEGED_PASSWORD);
    }

    @Test
    public void create_userProcedureUser_succeeds() throws SQLException, QueryMalformedException {

        /* mock */
        databaseAccessRepository.save(DATABASE_3_USER_1_WRITE_ALL_ACCESS);

        /* test */
        generic_user_insert("junit1", "junit1");
    }

    @Test
    public void delete_succeeds() throws QueryMalformedException, UserNotFoundException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, DatabaseMalformedException {

        /* mock */
        databaseRepository.save(DATABASE_1_SIMPLE);

        /* test */
        databaseService.delete(DATABASE_1_ID, USER_1_ID);
    }

    @Test
    public void visibility_succeeds() throws DatabaseNotFoundException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        databaseRepository.save(DATABASE_1_SIMPLE);

        /* test */
        final Database response = databaseService.visibility(DATABASE_1_ID, request);
        assertTrue(response.getIsPublic());
    }

    @Test
    public void transfer_succeeds() throws DatabaseNotFoundException, UserNotFoundException, SQLException,
            KeycloakRemoteException, AccessDeniedException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_2_USERNAME)
                .build();

        /* mock */
        databaseRepository.save(DATABASE_1_SIMPLE);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        when(userService.findByUsername(USER_2_USERNAME))
                .thenReturn(USER_2_DTO);

        /* test */
        final Database response = databaseService.transfer(DATABASE_1_ID, request);
        assertEquals(USER_2_ID, response.getOwnedBy());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_insert(String query, Long assertQueryId) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.mockGrantUserPermissions(CONTAINER_1, DATABASE_3, USER_1_USERNAME);

        /* test */
        final Long response = MariaDbConfig.mockSystemQueryInsert(DATABASE_3, query);
        assertNotNull(response);
        assertEquals(assertQueryId, response);
    }

    protected Database generic_create(DatabaseCreateDto createDto, Database database) throws Exception {

        /* test */
        final Database response = databaseService.create(createDto, USER_1_PRINCIPAL);
        assertEquals(database.getName(), response.getName());
        assertTrue(response.getInternalName().startsWith(database.getInternalName()));
        return response;
    }

    protected void generic_system_insert(String username, String password) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.mockGrantUserPermissions(CONTAINER_1, DATABASE_3, USER_1_USERNAME);

        /* test */
        final Long queryId = MariaDbConfig.mockSystemQueryInsert(DATABASE_3, QUERY_4_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

    protected void generic_user_insert(String username, String password) throws SQLException, QueryMalformedException {

        /* mock */
        mariaDbConfig.mockGrantUserPermissions(CONTAINER_1, DATABASE_3, USER_1_USERNAME);

        /* test */
        final Long queryId = MariaDbConfig.mockUserQueryInsert(DATABASE_3, QUERY_4_STATEMENT, username, password);
        assertEquals(1L, queryId);
    }

}
