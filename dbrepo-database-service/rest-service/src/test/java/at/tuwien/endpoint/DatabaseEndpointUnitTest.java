package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.*;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.DatabaseEndpoint;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.DatabaseIdxRepository;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.QueryStoreService;
import at.tuwien.test.BaseTest;
import com.rabbitmq.client.Channel;
import lombok.With;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MessageQueueService messageQueueService;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private QueryStoreService queryStoreService;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private IdentifierRepository identifierRepository;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @BeforeEach
    public void beforeEach() {
        DATABASE_1.setOwner(DATABASE_1_OWNER);
        DATABASE_2.setOwner(DATABASE_2_OWNER);
        DATABASE_3.setOwner(DATABASE_3_OWNER);
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, null, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void create_noRole_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_3_NAME)
                .isPublic(DATABASE_3_PUBLIC)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, null, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-database"})
    public void create_hasRoleForeign_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, null, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous_succeeds() {

        /* pre-condition */
        assertFalse(DATABASE_1_PUBLIC);

        /* test */
        list_generic(CONTAINER_1_ID, DATABASE_1_ID, CONTAINER_1, List.of(DATABASE_1), null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRole_succeeds() {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        list_generic(CONTAINER_3_ID, DATABASE_3_ID, CONTAINER_3, List.of(DATABASE_3), USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleForeign_succeeds() {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        list_generic(CONTAINER_3_ID, DATABASE_3_ID, CONTAINER_3, List.of(DATABASE_3), USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void visibility_anonymous_fails() {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            visibility_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, DATABASE_1_DTO, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-visibility"})
    public void visibility_hasRole_succeeds() throws NotAllowedException, DatabaseNotFoundException, UserNotFoundException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        visibility_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, DATABASE_1_DTO, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void visibility_noRole_fails() {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            visibility_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, DATABASE_1_DTO, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-database-visibility"})
    public void visibility_hasRoleForeign_fails() {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            visibility_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, DATABASE_1_DTO, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void transfer_noRole_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_4_USERNAME))
                .thenReturn(Optional.of(USER_4));

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            databaseEndpoint.transfer(CONTAINER_3_ID, DATABASE_3_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRoleForeign_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.transfer(CONTAINER_1_ID, DATABASE_1_ID, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException, NotAllowedException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.findByUsername(USER_4_USERNAME))
                .thenReturn(Optional.of(USER_4));
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        databaseEndpoint.transfer(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRoleUserNotExists_succeeds() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username("foobar")
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            databaseEndpoint.transfer(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymous_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, null);
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymousNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_hasRole_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        findById_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_hasRoleForeign_succeeds() throws AccessDeniedException,
            DatabaseNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        findById_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void delete_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, USER_1_USERNAME, USER_1_PRINCIPAL);
        });
    }

    @Test
    @Disabled
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-database"})
    public void delete_hasRole_succeeds() throws UserNotFoundException, BrokerVirtualHostGrantException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, DatabaseMalformedException {

        /* test */
        delete_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void list_generic(Long containerId, Long databaseId, Container container, List<Database> databases, Principal principal) {

        /* mock */
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        when(databaseRepository.findAll(containerId))
                .thenReturn(databases);
        when(identifierRepository.findByDatabaseId(databaseId))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<DatabaseBriefDto>> response = databaseEndpoint.list(containerId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<DatabaseBriefDto> body = response.getBody();
        assertEquals(databases.size(), body.size());
    }

    public void create_generic(Long containerId, Container container, Long databaseId, Database database,
                               DatabaseCreateDto data, Principal principal) throws UserNotFoundException,
            DatabaseNameExistsException, NotAllowedException, ContainerConnectionException, DatabaseMalformedException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, BrokerVirtualHostGrantException {

        /* mock */
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        if (database != null) {
            when(databaseRepository.findById(databaseId))
                    .thenReturn(Optional.of(database));
        } else {
            when(databaseRepository.findById(databaseId))
                    .thenReturn(Optional.empty());
        }
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);
        doNothing()
                .when(messageQueueService)
                .createExchange(database, principal);
        doNothing()
                .when(queryStoreService)
                .create(containerId, databaseId, principal);
        doNothing()
                .when(messageQueueService)
                .updatePermissions(principal);
        when(databaseAccessRepository.save(any(DatabaseAccess.class)))
                .thenReturn(DATABASE_1_RESEARCHER_WRITE_ALL_ACCESS);

        /* test */
        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.create(containerId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void visibility_generic(Long containerId, Container container, Long databaseId, Database database,
                                   DatabaseDto dto, DatabaseModifyVisibilityDto data, Principal principal)
            throws NotAllowedException, DatabaseNotFoundException, UserNotFoundException {

        /* mock */
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(dto);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.visibility(containerId, databaseId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void findById_generic(Long containerId, Container container, Long databaseId, Database database,
                                 Principal principal) throws DatabaseNotFoundException, AccessDeniedException {

        /* mock */
        if (container != null) {
            when(containerRepository.findById(containerId))
                    .thenReturn(Optional.of(container));
        } else {
            when(containerRepository.findById(containerId))
                    .thenReturn(Optional.empty());
        }
        if (database != null) {
            when(databaseRepository.findById(databaseId))
                    .thenReturn(Optional.of(database));
        } else {
            when(databaseRepository.findById(databaseId))
                    .thenReturn(Optional.empty());
        }

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.findById(containerId, databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void delete_generic(Long containerId, Container container, Long databaseId, Database database,
                               String username, Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, DatabaseMalformedException, BrokerVirtualHostGrantException {

        /* mock */
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (username != null) {
            when(databaseRepository.findPublicOrMine(containerId, databaseId, username))
                    .thenReturn(Optional.of(database));
        }

        /* test */
        final ResponseEntity<?> response = databaseEndpoint.delete(containerId, databaseId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }
}
