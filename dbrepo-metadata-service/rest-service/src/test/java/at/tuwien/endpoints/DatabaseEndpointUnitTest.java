package at.tuwien.endpoints;

import at.tuwien.service.StorageService;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.database.*;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.repository.UserRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.ContainerService;
import at.tuwien.service.BrokerService;
import at.tuwien.service.impl.DatabaseServiceImpl;
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
public class DatabaseEndpointUnitTest extends AbstractUnitTest {

    @MockBean
    private BrokerService messageQueueService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private KeycloakGateway keycloakGateway;

    @MockBean
    private ContainerService containerService;

    @MockBean
    private DatabaseServiceImpl databaseService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private StorageService storageService;

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
            create_generic(request, null, null);
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
            create_generic(request, USER_4_PRINCIPAL, USER_4);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database"})
    public void create_succeeds() throws ServiceException, ServiceConnectionException, UserNotFoundException,
            DatabaseNotFoundException, ContainerNotFoundException, SearchServiceException,
            SearchServiceConnectionException, AuthServiceException, AuthServiceConnectionException,
            CredentialsInvalidException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        when(containerService.find(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1);
        when(databaseService.create(request, USER_1))
                .thenReturn(DATABASE_1);
        doNothing()
                .when(messageQueueService)
                .setVirtualHostPermissions(USER_1);
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        create_generic(request, USER_1_PRINCIPAL, USER_1);
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous_succeeds() throws DatabaseNotFoundException {

        /* pre-condition */
        assertFalse(DATABASE_1_PUBLIC);

        /* test */
        list_generic(List.of(DATABASE_1), null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRole_succeeds() throws DatabaseNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        list_generic(List.of(DATABASE_3), null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleForeign_succeeds() throws DatabaseNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        list_generic(List.of(DATABASE_3), null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleFilter_succeeds() throws DatabaseNotFoundException {

        /* test */
        list_generic(List.of(DATABASE_3), DATABASE_3_INTERNALNAME);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleFilterNoResult_succeeds() throws DatabaseNotFoundException {

        /* test */
        list_generic(List.of(), "i_do_not_exist");
    }

    @Test
    @WithAnonymousUser
    public void visibility_anonymous_fails() {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            visibility_generic(DATABASE_1_ID, DATABASE_1, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-visibility"})
    public void visibility_hasRole_succeeds() throws NotAllowedException, UserNotFoundException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException, AuthServiceException,
            AuthServiceConnectionException, CredentialsInvalidException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        visibility_generic(DATABASE_1_ID, DATABASE_1, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void visibility_noRole_fails() {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            visibility_generic(DATABASE_1_ID, DATABASE_1, request, USER_4_PRINCIPAL);
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
            visibility_generic(DATABASE_1_ID, DATABASE_1, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void modifyImage_noRole_fails() {
        final DatabaseModifyImageDto request = DatabaseModifyImageDto.builder()
                .key("s3key_here")
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            databaseEndpoint.modifyImage(DATABASE_3_ID, request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-image"})
    public void modifyImage_hasRole_succeeds() throws NotAllowedException, UserNotFoundException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException,
            StorageUnavailableException, StorageNotFoundException {
        final DatabaseModifyImageDto request = DatabaseModifyImageDto.builder()
                .key("s3key_here")
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(storageService.getBytes(request.getKey()))
                .thenReturn(new byte[]{});

        /* test */
        databaseEndpoint.modifyImage(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void transfer_noRole_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .id(USER_4_ID)
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
                .id(USER_4_ID)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));
        when(userRepository.findById(USER_4_ID))
                .thenReturn(Optional.of(USER_4));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.transfer(DATABASE_1_ID, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRole_succeeds() throws ServiceConnectionException, ServiceException,
            NotAllowedException, UserNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, AuthServiceException, AuthServiceConnectionException,
            CredentialsInvalidException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .id(USER_4_ID)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(userRepository.findById(USER_4_ID))
                .thenReturn(Optional.of(USER_4));

        /* test */
        databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRoleUserNotExists_succeeds() throws DatabaseNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .id(UUID.randomUUID())
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymous_succeeds() throws ServiceException, ServiceConnectionException,
            DatabaseNotFoundException, ExchangeNotFoundException {

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
    public void findById_hasRole_succeeds() throws ServiceException, ServiceConnectionException,
            DatabaseNotFoundException, ExchangeNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        findById_generic(DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_hasRoleForeign_succeeds() throws ServiceException, ServiceConnectionException,
            DatabaseNotFoundException, ExchangeNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        findById_generic(DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_ownerSeesAccessRights_succeeds() throws ServiceException, ServiceConnectionException,
            DatabaseNotFoundException, ExchangeNotFoundException {

        /* mock */
        when(accessService.list(DATABASE_1))
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

    public void list_generic(List<Database> databases, String internalName) throws DatabaseNotFoundException {

        /* mock */
        when(databaseService.findAll())
                .thenReturn(databases);
        if (internalName != null) {
            if (!databases.isEmpty()) {
                when(databaseService.findByInternalName(internalName))
                        .thenReturn(databases.get(0));
            } else {
                doThrow(DatabaseNotFoundException.class)
                        .when(databaseService)
                        .findByInternalName(internalName);
            }
        }

        /* test */
        final ResponseEntity<List<DatabaseBriefDto>> response = databaseEndpoint.list(internalName);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<DatabaseBriefDto> body = response.getBody();
        assertEquals(databases.size(), body.size());
    }

    public void create_generic(DatabaseCreateDto data, Principal principal, User user) throws ServiceException,
            ServiceConnectionException, UserNotFoundException, DatabaseNotFoundException, ContainerNotFoundException,
            SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(messageQueueService)
                .setVirtualHostPermissions(user);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void visibility_generic(Long databaseId, Database database, DatabaseModifyVisibilityDto data,
                                   Principal principal) throws NotAllowedException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {

        /* mock */
        if (database != null) {
            when(databaseService.findById(databaseId))
                    .thenReturn(database);
            when(databaseService.modifyVisibility(database, data))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .findById(databaseId);
        }

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.visibility(databaseId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public DatabaseDto findById_generic(Long databaseId, Database database, Principal principal)
            throws ServiceException, ServiceConnectionException, DatabaseNotFoundException, ExchangeNotFoundException {

        /* mock */
        if (database != null) {
            when(databaseService.findById(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .findById(databaseId);
        }

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.findById(databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final DatabaseDto body = response.getBody();
        assertNotNull(body);
        return body;
    }

}
