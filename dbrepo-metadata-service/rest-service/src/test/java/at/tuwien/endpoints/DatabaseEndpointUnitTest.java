package at.tuwien.endpoints;

import at.tuwien.api.database.*;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.service.*;
import at.tuwien.service.impl.DatabaseServiceImpl;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
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
    private UserService userService;

    @MockBean
    private StorageService storageService;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
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
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, USER_4_PRINCIPAL, USER_4);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database"})
    public void create_succeeds() throws DataServiceException, DataServiceConnectionException, UserNotFoundException,
            DatabaseNotFoundException, ContainerNotFoundException, SearchServiceException,
            SearchServiceConnectionException, AuthServiceException, AuthServiceConnectionException,
            BrokerServiceException, BrokerServiceConnectionException, ContainerQuotaException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        when(containerService.find(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);
        when(userService.findAllInternalUsers())
                .thenReturn(List.of(USER_LOCAL));
        when(databaseService.create(CONTAINER_1, request, USER_1, List.of(USER_LOCAL)))
                .thenReturn(DATABASE_1);

        /* test */
        create_generic(request, USER_1_PRINCIPAL, USER_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database"})
    public void create_quotaExceeded_fails() throws UserNotFoundException, ContainerNotFoundException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .cid(CONTAINER_4_ID)
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        when(containerService.find(CONTAINER_4_ID))
                .thenReturn(CONTAINER_4);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);

        /* test */
        assertThrows(ContainerQuotaException.class, () -> {
            create_generic(request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithAnonymousUser
    public void refreshTableMetadata_anonymous_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.refreshTableMetadata(DATABASE_1_ID, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void refreshTableMetadata_noRole_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.refreshTableMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"find-database"})
    public void refreshTableMetadata_notOwner_fails() throws UserNotFoundException, TableNotFoundException,
            SearchServiceException, MalformedException, DataServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_2_ID))
                .thenReturn(USER_2);
        when(databaseService.updateTableMetadata(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.refreshTableMetadata(DATABASE_1_ID, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void refreshTableMetadata_succeeds() throws UserNotFoundException, TableNotFoundException,
            SearchServiceException, MalformedException, DataServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException, NotAllowedException,
            QueryNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);
        when(databaseService.updateTableMetadata(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.refreshTableMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void refreshViewMetadata_succeeds() throws UserNotFoundException, SearchServiceException,
            NotAllowedException, DataServiceException, QueryNotFoundException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException, ViewNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);
        when(databaseService.updateViewMetadata(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"find-database"})
    public void refreshViewMetadata_notOwner_fails() throws UserNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_2_ID))
                .thenReturn(USER_2);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void refreshViewMetadata_noRole_fails() throws UserNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void refreshViewMetadata_anonymous_fails() throws UserNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous_succeeds() throws DatabaseNotFoundException, UserNotFoundException {

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublic())
                .thenReturn(List.of(DATABASE_1));

        /* test */
        list_generic(null, null, 1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRole_succeeds() throws DatabaseNotFoundException, UserNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicOrReadAccess(any(UUID.class)))
                .thenReturn(List.of(DATABASE_3));

        /* test */
        list_generic(null, USER_1_PRINCIPAL, 1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleForeign_succeeds() throws DatabaseNotFoundException, UserNotFoundException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicOrReadAccess(USER_1_ID))
                .thenReturn(List.of(DATABASE_3));

        /* test */
        list_generic(null, USER_1_PRINCIPAL, 1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleFilter_succeeds() throws DatabaseNotFoundException, UserNotFoundException {

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicOrReadAccessByInternalName(USER_1_ID, DATABASE_3_INTERNALNAME))
                .thenReturn(List.of(DATABASE_3));

        /* test */
        list_generic(DATABASE_3_INTERNALNAME, USER_1_PRINCIPAL, 1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleFilterNoResult_succeeds() throws DatabaseNotFoundException, UserNotFoundException {

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicOrReadAccessByInternalName(USER_1_ID, "i_do_not_exist"))
                .thenReturn(List.of());

        /* test */
        list_generic("i_do_not_exist", USER_1_PRINCIPAL, 0);
    }

    @Test
    @WithAnonymousUser
    public void visibility_anonymous_fails() {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            visibility_generic(DATABASE_1_ID, DATABASE_1, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-visibility"})
    public void visibility_hasRole_succeeds() throws NotAllowedException, UserNotFoundException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException, AuthServiceException,
            AuthServiceConnectionException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);

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
        assertThrows(AccessDeniedException.class, () -> {
            visibility_generic(DATABASE_1_ID, DATABASE_1, request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-database-visibility"})
    public void visibility_hasRoleForeign_fails() throws UserNotFoundException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        when(userService.findById(USER_2_ID))
                .thenReturn(USER_2);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            visibility_generic(DATABASE_1_ID, DATABASE_1, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void modifyImage_noRole_fails() {
        final DatabaseModifyImageDto request = DatabaseModifyImageDto.builder()
                .key("s3key_here")
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.modifyImage(DATABASE_3_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-database-image"})
    public void modifyImage_notOwner_fails() throws DatabaseNotFoundException, UserNotFoundException {
        final DatabaseModifyImageDto request = DatabaseModifyImageDto.builder()
                .key("s3key_here")
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_3_ID))
                .thenReturn(DATABASE_3);
        when(userService.findById(USER_2_ID))
                .thenReturn(USER_2);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.modifyImage(DATABASE_3_ID, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-image"})
    public void modifyImage_succeeds() throws NotAllowedException, UserNotFoundException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException,
            StorageUnavailableException, StorageNotFoundException {
        final DatabaseModifyImageDto request = DatabaseModifyImageDto.builder()
                .key("s3key_here")
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);
        when(storageService.getBytes(request.getKey()))
                .thenReturn(new byte[]{1, 2, 3, 4, 5});

        /* test */
        databaseEndpoint.modifyImage(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-image"})
    public void modifyImage_empty_succeeds() throws NotAllowedException, UserNotFoundException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException,
            StorageUnavailableException, StorageNotFoundException {
        final DatabaseModifyImageDto request = DatabaseModifyImageDto.builder()
                .key(null)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);

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
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.transfer(DATABASE_3_ID, request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRoleForeign_fails() throws DatabaseNotFoundException, UserNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .id(USER_4_ID)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findById(USER_2_ID))
                .thenReturn(USER_2);
        when(userService.findById(USER_4_ID))
                .thenReturn(USER_4);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.transfer(DATABASE_1_ID, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRole_succeeds() throws DataServiceConnectionException, DataServiceException,
            NotAllowedException, UserNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException, AuthServiceException, AuthServiceConnectionException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .id(USER_4_ID)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(keycloakGateway.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_KEYCLOAK_DTO);
        when(userService.findById(USER_1_ID))
                .thenReturn(USER_1);
        when(userService.findById(USER_4_ID))
                .thenReturn(USER_4);

        /* test */
        databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRoleUserNotExists_succeeds() throws DatabaseNotFoundException, UserNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .id(UUID.randomUUID())
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        doThrow(UserNotFoundException.class)
                .when(userService)
                .findById(any(UUID.class));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            findById_generic(DATABASE_1_ID, DATABASE_1, null);
        });
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
    public void findById_hasRole_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, ExchangeNotFoundException, UserNotFoundException, NotAllowedException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        findById_generic(DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_hasRoleForeign_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, ExchangeNotFoundException, UserNotFoundException, NotAllowedException {

        /* pre-condition */
        assertTrue(DATABASE_3_PUBLIC);

        /* test */
        findById_generic(DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void findById_ownerSeesAccessRights_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, ExchangeNotFoundException, UserNotFoundException, NotAllowedException {

        /* mock */
        when(accessService.list(DATABASE_1))
                .thenReturn(List.of(DATABASE_1_USER_1_WRITE_ALL_ACCESS, DATABASE_1_USER_2_READ_ACCESS));

        /* test */
        final DatabaseDto response = findById_generic(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
        final List<DatabaseAccessDto> accessList = response.getAccesses();
        assertNotNull(accessList);
        assertEquals(3, accessList.size());
    }

    @Test
    @WithAnonymousUser
    public void findPreviewImage_anonymous_succeeds() throws DatabaseNotFoundException {

        /* test */
        final ResponseEntity<byte[]> response = findPreviewImage_generic(DATABASE_1_ID, DATABASE_1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("image/webp"), response.getHeaders().getContentType());
        final byte[] body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findPreviewImage_noRoles_succeeds() throws DatabaseNotFoundException {

        /* test */
        final ResponseEntity<byte[]> response = findPreviewImage_generic(DATABASE_1_ID, DATABASE_1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("image/webp"), response.getHeaders().getContentType());
        final byte[] body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithAnonymousUser
    public void findPreviewImage_noImage_succeeds() throws DatabaseNotFoundException {

        /* test */
        final ResponseEntity<byte[]> response = findPreviewImage_generic(DATABASE_2_ID, DATABASE_2);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.parseMediaType("image/webp"), response.getHeaders().getContentType());
        final byte[] body = response.getBody();
        assertNull(body);
    }

    @Test
    @WithAnonymousUser
    public void findPreviewImage_notFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            findPreviewImage_generic(DATABASE_1_ID, null);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void list_generic(String internalName, Principal principal, Integer expectedSize)
            throws DatabaseNotFoundException, UserNotFoundException {

        /* test */
        final ResponseEntity<List<DatabaseBriefDto>> response = databaseEndpoint.list(internalName, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<DatabaseBriefDto> body = response.getBody();
        assertEquals(expectedSize, body.size());
    }

    public void create_generic(DatabaseCreateDto data, Principal principal, User user) throws DataServiceException,
            DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            ContainerNotFoundException, SearchServiceException, SearchServiceConnectionException,
            BrokerServiceException, BrokerServiceConnectionException, ContainerQuotaException {

        /* mock */
        doNothing()
                .when(messageQueueService)
                .setVirtualHostPermissions(user);
        when(databaseService.findById(anyLong()))
                .thenReturn(DATABASE_1);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void visibility_generic(Long databaseId, Database database, DatabaseModifyVisibilityDto data,
                                   Principal principal) throws NotAllowedException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, UserNotFoundException {

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
            throws DataServiceConnectionException, DatabaseNotFoundException, ExchangeNotFoundException,
            DataServiceException, UserNotFoundException, NotAllowedException {

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

    public ResponseEntity<byte[]> findPreviewImage_generic(Long databaseId, Database database) throws DatabaseNotFoundException {

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
        return databaseEndpoint.findPreviewImage(databaseId);
    }

}
