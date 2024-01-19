package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.*;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.ContainerService;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.QueryStoreService;
import at.tuwien.service.impl.MariaDbServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class DatabaseEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private MessageQueueService messageQueueService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private KeycloakGateway keycloakGateway;

    @MockBean
    private ContainerService containerService;

    @MockBean
    private MariaDbServiceImpl databaseService;

    @MockBean
    private QueryStoreService queryStoreService;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(DATABASE_1_ID, request, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .cid(CONTAINER_3_ID)
                .name(DATABASE_3_NAME)
                .isPublic(DATABASE_3_PUBLIC)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(DATABASE_3_ID, request, USER_4_USERNAME, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database"})
    public void create_succeeds() throws UserNotFoundException, BrokerVirtualHostGrantException,
            DatabaseNameExistsException, NotAllowedException, ContainerConnectionException, DatabaseMalformedException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, AmqpException, BrokerVirtualHostModificationException, ContainerNotFoundException,
            KeycloakRemoteException, AccessDeniedException, BrokerRemoteException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        when(containerService.find(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1);
        when(databaseService.create(request, USER_1_PRINCIPAL))
                .thenReturn(DATABASE_1);
        doNothing()
                .when(messageQueueService)
                .createUser(USER_1_USERNAME, USER_1_PASSWORD);
        doNothing()
                .when(messageQueueService)
                .setVirtualHostPermissions(USER_1_USERNAME);
        doNothing()
                .when(queryStoreService)
                .create(DATABASE_1_ID, USER_1_PRINCIPAL);
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        create_generic(DATABASE_1_ID, request, USER_1_USERNAME, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous_succeeds() throws UserNotFoundException {

        /* pre-condition */
        assertFalse(DATABASE_1_PUBLIC);

        /* test */
        list_generic(DATABASE_1_ID, CONTAINER_1, List.of(DATABASE_1), null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRole_succeeds() throws UserNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        list_generic(DATABASE_3_ID, CONTAINER_3, List.of(DATABASE_3), USER_1_PRINCIPAL, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleForeign_succeeds() throws UserNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        list_generic(DATABASE_3_ID, CONTAINER_3, List.of(DATABASE_3), USER_1_PRINCIPAL, null);
    }

    @Test
    @WithAnonymousUser
    public void count_anonymous_succeeds() throws UserNotFoundException {

        /* pre-condition */
        assertFalse(DATABASE_1_PUBLIC);

        /* test */
        count_generic(DATABASE_1_ID, List.of(DATABASE_1), null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void count_hasRole_succeeds() throws UserNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        count_generic(DATABASE_3_ID, List.of(DATABASE_3), USER_1_PRINCIPAL, USER_1_ID, "access");
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void count_hasRoleForeign_succeeds() throws UserNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        count_generic(DATABASE_3_ID, List.of(DATABASE_3), USER_1_PRINCIPAL, USER_1_ID, "access");
    }

    @Test
    @WithAnonymousUser
    public void visibility_anonymous_fails() {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            visibility_generic(DATABASE_1_ID, DATABASE_1, DATABASE_1_DTO, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-visibility"})
    public void visibility_hasRole_succeeds() throws NotAllowedException, DatabaseNotFoundException,
            UserNotFoundException, KeycloakRemoteException, AccessDeniedException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        visibility_generic(DATABASE_1_ID, DATABASE_1, DATABASE_1_DTO, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void visibility_noRole_fails() {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            visibility_generic(DATABASE_1_ID, DATABASE_1, DATABASE_1_DTO, request, USER_4_PRINCIPAL);
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
            visibility_generic(DATABASE_1_ID, DATABASE_1, DATABASE_1_DTO, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void transfer_noRole_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            databaseEndpoint.transfer(DATABASE_3_ID, request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRoleForeign_fails() throws DatabaseNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.transfer(DATABASE_1_ID, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRole_succeeds() throws UserNotFoundException, DatabaseNotFoundException,
            NotAllowedException, KeycloakRemoteException, AccessDeniedException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRoleUserNotExists_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            KeycloakRemoteException, AccessDeniedException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username("foobar")
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        doThrow(UserNotFoundException.class)
                .when(databaseService)
                .transfer(DATABASE_1_ID, request);

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymous_succeeds() throws NotAllowedException, DatabaseNotFoundException,
            ExchangeNotFoundException, BrokerRemoteException {

        /* test */
        findById_generic(DATABASE_1_ID, DATABASE_1, null);
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymousNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            findById_generic(DATABASE_1_ID, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_hasRole_succeeds() throws NotAllowedException, DatabaseNotFoundException,
            ExchangeNotFoundException, BrokerRemoteException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        findById_generic(DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_hasRoleForeign_succeeds() throws NotAllowedException, DatabaseNotFoundException,
            ExchangeNotFoundException, BrokerRemoteException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        findById_generic(DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_ownerSeesAccessRights_succeeds() throws NotAllowedException, DatabaseNotFoundException,
            ExchangeNotFoundException, BrokerRemoteException {

        /* mock */
        when(accessService.list(DATABASE_1_ID))
                .thenReturn(List.of(DATABASE_1_USER_1_WRITE_ALL_ACCESS, DATABASE_1_USER_2_READ_ACCESS));

        /* test */
        final DatabaseDto response = findById_generic(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
        final List<DatabaseAccessDto> accessList = response.getAccesses();
        assertNotNull(accessList);
        assertEquals(2, accessList.size());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void list_generic(Long databaseId, Container container, List<Database> databases, Principal principal,
                             String filter)
            throws UserNotFoundException {

        /* mock */
        when(identifierRepository.findByDatabaseId(databaseId))
                .thenReturn(List.of());
        when(databaseService.findAll())
                .thenReturn(databases);

        /* test */
        final ResponseEntity<List<DatabaseDto>> response = databaseEndpoint.list(principal, filter);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<DatabaseDto> body = response.getBody();
        assertEquals(databases.size(), body.size());
    }

    public void count_generic(Long databaseId, List<Database> databases, Principal principal, UUID userId,
                              String filter) throws UserNotFoundException {

        /* mock */
        when(identifierRepository.findByDatabaseId(databaseId))
                .thenReturn(List.of());
        if (principal != null) {
            when(databaseService.findAccess(userId))
                    .thenReturn(databases);
        } else {
            when(databaseService.findAll())
                    .thenReturn(databases);
        }

        /* test */
        final ResponseEntity<List<DatabaseDto>> response = databaseEndpoint.count(principal, filter);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        final List<String> headerCount = response.getHeaders().get("x-count");
        assertNotNull(headerCount);
        assertEquals(headerCount.size(), 1);
        assertEquals(headerCount.get(0), "" + databases.size());
    }

    public void create_generic(Long databaseId, DatabaseCreateDto data, String username,
                               Principal principal) throws UserNotFoundException, DatabaseNameExistsException,
            NotAllowedException, ContainerConnectionException, DatabaseMalformedException, QueryStoreException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostModificationException, ContainerNotFoundException,
            BrokerVirtualHostGrantException, KeycloakRemoteException, AccessDeniedException, BrokerRemoteException {

        /* mock */
        doNothing()
                .when(queryStoreService)
                .create(databaseId, principal);
        doNothing()
                .when(messageQueueService)
                .setVirtualHostPermissions(username);

        /* test */
        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void visibility_generic(Long databaseId, Database database, DatabaseDto dto,
                                   DatabaseModifyVisibilityDto data, Principal principal) throws NotAllowedException,
            DatabaseNotFoundException, UserNotFoundException {

        /* mock */
        if (database != null) {
            when(databaseService.findById(databaseId))
                    .thenReturn(database);
            when(databaseService.visibility(databaseId, data))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .findById(databaseId);
        }
        when(databaseIdxRepository.save(any(DatabaseDto.class)))
                .thenReturn(dto);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.visibility(databaseId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public DatabaseDto findById_generic(Long databaseId, Database database, Principal principal)
            throws DatabaseNotFoundException, ExchangeNotFoundException, BrokerRemoteException {

        /* mock */
        if (database != null) {
            when(databaseService.findById(databaseId))
                    .thenReturn(database);
            when(messageQueueService.findExchange(EXCHANGE_DBREPO_NAME))
                    .thenReturn(EXCHANGE_DBREPO_DTO);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .findById(databaseId);
            doThrow(ExchangeNotFoundException.class)
                    .when(messageQueueService)
                    .findExchange(EXCHANGE_DBREPO_NAME);
        }

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.findById(databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final DatabaseDto body = response.getBody();
        assertNotNull(body);
        return body;
    }

}
