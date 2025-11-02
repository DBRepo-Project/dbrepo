package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.*;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.*;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseEndpointUnitTest extends BaseTest {

    @MockitoBean
    private BrokerService messageQueueService;

    @MockitoBean
    private AccessService accessService;

    @MockitoBean
    private ContainerService containerService;

    @MockitoBean
    private DatabaseServiceImpl databaseService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private DashboardService dashboardService;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {
        final CreateDatabaseDto request = CreateDatabaseDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1.getName())
                .isPublic(DATABASE_1.getIsPublic())
                .isSchemaPublic(DATABASE_1.getIsSchemaPublic())
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {
        final CreateDatabaseDto request = CreateDatabaseDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_3.getName())
                .isPublic(DATABASE_3.getIsPublic())
                .isSchemaPublic(DATABASE_3.getIsSchemaPublic())
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, USER_4_PRINCIPAL, USER_4_USERNAME);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database"})
    public void create_succeeds() throws DataServiceException, DataServiceConnectionException, UserNotFoundException,
            DatabaseNotFoundException, ContainerNotFoundException, SearchServiceException,
            SearchServiceConnectionException, AuthServiceException, AuthServiceConnectionException,
            BrokerServiceException, BrokerServiceConnectionException, ContainerQuotaException,
            DashboardServiceException, DashboardServiceConnectionException, NotAllowedException {
        final CreateDatabaseDto request = CreateDatabaseDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1.getName())
                .isPublic(DATABASE_1.getIsPublic())
                .build();

        /* mock */
        when(containerService.find(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        when(databaseService.create(CONTAINER_1, request, USER_1_DTO))
                .thenReturn(DATABASE_1);

        /* test */
        create_generic(request, USER_1_PRINCIPAL, USER_1_USERNAME);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database"})
    public void create_quotaExceeded_fails() throws UserNotFoundException, ContainerNotFoundException,
            NotAllowedException {
        final CreateDatabaseDto request = CreateDatabaseDto.builder()
                .cid(CONTAINER_4.getId())
                .name(DATABASE_1.getName())
                .isPublic(DATABASE_1.getIsPublic())
                .build();

        /* mock */
        when(containerService.find(CONTAINER_4.getId()))
                .thenReturn(CONTAINER_4);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        assertThrows(ContainerQuotaException.class, () -> {
            create_generic(request, USER_1_PRINCIPAL, USER_1_USERNAME);
        });
    }

    @Test
    @WithAnonymousUser
    public void refreshTableMetadata_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.refreshTableMetadata(DATABASE_1_ID, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void refreshTableMetadata_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.refreshTableMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"find-database"})
    public void refreshTableMetadata_notOwner_fails() throws UserNotFoundException, TableNotFoundException,
            SearchServiceException, MalformedException, DataServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException, NotAllowedException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_2_USERNAME))
                .thenReturn(USER_2_DTO);
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
            SearchServiceConnectionException, DataServiceConnectionException, NotAllowedException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        when(databaseService.updateTableMetadata(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.refreshTableMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-database"})
    public void refreshViewMetadata_succeeds() throws UserNotFoundException, SearchServiceException,
            NotAllowedException, DataServiceException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException, ViewNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        when(databaseService.updateViewMetadata(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"find-database"})
    public void refreshViewMetadata_notOwner_fails() throws UserNotFoundException, DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_2_USERNAME))
                .thenReturn(USER_2_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void refreshViewMetadata_noRole_fails() throws UserNotFoundException, DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void refreshViewMetadata_anonymous_fails() throws UserNotFoundException, DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.refreshViewMetadata(DATABASE_1_ID, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous_succeeds() {

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublic())
                .thenReturn(List.of(DATABASE_1));

        /* test */
        list_generic(null, null, 1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRole_succeeds() {

        /* pre-condition */
        assertTrue(DATABASE_3.getIsPublic());

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicOrReadAccess(anyString()))
                .thenReturn(List.of(DATABASE_3));

        /* test */
        list_generic(null, USER_1_PRINCIPAL, 1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleForeign_succeeds() {

        /* pre-condition */
        assertTrue(DATABASE_3.getIsPublic());

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicOrReadAccess(USER_1_USERNAME))
                .thenReturn(List.of(DATABASE_3));

        /* test */
        list_generic(null, USER_1_PRINCIPAL, 1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleFilter_succeeds() {

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicOrReadAccessByInternalName(USER_1_USERNAME, DATABASE_3.getInternalName()))
                .thenReturn(List.of(DATABASE_3));

        /* test */
        list_generic(DATABASE_3.getInternalName(), USER_1_PRINCIPAL, 1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-databases"})
    public void list_hasRoleFilterNoResult_succeeds() {

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicOrReadAccessByInternalName(USER_1_USERNAME, "i_do_not_exist"))
                .thenReturn(List.of());

        /* test */
        list_generic("i_do_not_exist", USER_1_PRINCIPAL, 0);
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void list_hasSystemRole_succeeds() {

        /* mock */
        when(databaseService.findAll())
                .thenReturn(List.of(DATABASE_1, DATABASE_2, DATABASE_3, DATABASE_4));

        /* test */
        list_generic(null, USER_LOCAL_ADMIN_PRINCIPAL, 4);
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void list_hasSystemRoleFilterByName_succeeds() {

        /* mock */
        when(databaseService.findByInternalName(DATABASE_1.getInternalName()))
                .thenReturn(List.of(DATABASE_1));

        /* test */
        list_generic(DATABASE_1.getInternalName(), USER_LOCAL_ADMIN_PRINCIPAL, 1);
    }

    @Test
    @WithAnonymousUser
    public void list_filterNoResult_succeeds() {

        /* mock */
        when(databaseService.findAllPublicOrSchemaPublicByInternalName("i_do_not_exist"))
                .thenReturn(List.of());

        /* test */
        list_generic("i_do_not_exist", null, 0);
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
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

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
    public void visibility_hasRoleForeign_fails() throws UserNotFoundException, NotAllowedException {
        final DatabaseModifyVisibilityDto request = DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .build();

        /* mock */
        when(userService.findByUsername(USER_2_USERNAME))
                .thenReturn(USER_2_DTO);

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
    public void modifyImage_notOwner_fails() throws DatabaseNotFoundException, UserNotFoundException,
            NotAllowedException {
        final DatabaseModifyImageDto request = DatabaseModifyImageDto.builder()
                .key("s3key_here")
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_3_ID))
                .thenReturn(DATABASE_3);
        when(userService.findByUsername(USER_2_USERNAME))
                .thenReturn(USER_2_DTO);

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
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
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
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);

        /* test */
        databaseEndpoint.modifyImage(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void transfer_noRole_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            databaseEndpoint.transfer(DATABASE_3_ID, request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRoleForeign_fails() throws DatabaseNotFoundException, UserNotFoundException,
            NotAllowedException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_2_USERNAME))
                .thenReturn(USER_2_DTO);
        when(userService.findByUsername(USER_4_USERNAME))
                .thenReturn(USER_4_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            databaseEndpoint.transfer(DATABASE_1_ID, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-database-owner"})
    public void transfer_hasRole_succeeds() throws DataServiceConnectionException, DataServiceException,
            NotAllowedException, UserNotFoundException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .username(USER_4_USERNAME)
                .build();

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1_DTO);
        when(userService.findByUsername(USER_4_USERNAME))
                .thenReturn(USER_4_DTO);

        /* test */
        databaseEndpoint.transfer(DATABASE_1_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymousPrivateSchemaNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            findById_generic(DATABASE_1_ID, DATABASE_1, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymousPublicSchemaNoAccess_succeeds() throws NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        final ResponseEntity<DatabaseDto> response = findById_generic(DATABASE_2_ID, DATABASE_2, null);
        final DatabaseDto database = response.getBody();
        assertNotNull(database);
        assertEquals(3, database.getTables().size());
        assertEquals(1, database.getViews().size());
        assertEquals(0, database.getAccesses().size());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void findById_privateSchemaNoAccessInternalUser_succeeds() throws NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        final ResponseEntity<DatabaseDto> response = findById_generic(DATABASE_3_ID, DATABASE_3, USER_LOCAL_ADMIN_PRINCIPAL);
        final DatabaseDto database = response.getBody();
        assertNotNull(database);
        assertEquals(1, database.getTables().size());
        assertEquals(1, database.getViews().size());
        assertNotEquals(0, database.getAccesses().size());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void findById_privateSchemaPrivateDataNoAccessInternalUser_succeeds() throws NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        final ResponseEntity<DatabaseDto> response = findById_generic(DATABASE_1_ID, DATABASE_1, USER_LOCAL_ADMIN_PRINCIPAL);
        final DatabaseDto database = response.getBody();
        assertNotNull(database);
        assertEquals(4, database.getTables().size());
        assertEquals(3, database.getViews().size());
        assertNotEquals(0, database.getAccesses().size());
    }

    @Test
    @WithAnonymousUser
    public void findById_privateSchema_fails() {

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
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_ownerSeesAccessRights_succeeds() throws DatabaseNotFoundException, NotAllowedException {

        /* mock */
        when(accessService.list(DATABASE_1))
                .thenReturn(List.of(DATABASE_1.getAccesses().get(0), DATABASE_1.getAccesses().get(1)));

        /* test */
        final ResponseEntity<DatabaseDto> response = findById_generic(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
        final DatabaseDto database = response.getBody();
        assertNotNull(database);
        assertEquals(4, database.getTables().size());
        assertEquals(3, database.getViews().size());
        assertEquals(3, database.getAccesses().size());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findById_hiddenAccessRights_succeeds() throws DatabaseNotFoundException, NotAllowedException {

        /* mock */
        when(accessService.list(DATABASE_1))
                .thenReturn(List.of(DATABASE_1.getAccesses().get(0), DATABASE_1.getAccesses().get(1)));

        /* test */
        final ResponseEntity<DatabaseDto> response = findById_generic(DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL);
        final DatabaseDto database = response.getBody();
        assertNotNull(database);
        assertEquals(4, database.getTables().size());
        assertEquals(3, database.getViews().size());
        assertEquals(0, database.getAccesses().size());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_hiddenAccessRightsSeesOwn_succeeds() throws DatabaseNotFoundException, NotAllowedException {

        /* mock */
        when(accessService.list(DATABASE_1))
                .thenReturn(List.of(DATABASE_1.getAccesses().get(0), DATABASE_1.getAccesses().get(1)));

        /* test */
        final ResponseEntity<DatabaseDto> response = findById_generic(DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
        final DatabaseDto database = response.getBody();
        assertNotNull(database);
        assertEquals(4, database.getTables().size());
        assertEquals(3, database.getViews().size());
        assertEquals(3, database.getAccesses().size());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_privateDataPrivateSchemaNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            findById_generic(DATABASE_1_ID, DATABASE_1, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_anonymousPrivateDataPrivateSchema_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            findById_generic(DATABASE_1_ID, DATABASE_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void findById_system_succeeds() throws NotAllowedException, DatabaseNotFoundException {

        /* test */
        final ResponseEntity<DatabaseDto> response = findById_generic(DATABASE_1_ID, DATABASE_1, USER_LOCAL_ADMIN_PRINCIPAL);
        final HttpHeaders headers = response.getHeaders();
        assertEquals(List.of(CONTAINER_1_HOST), headers.get("X-Host"));
        assertEquals(List.of("" + CONTAINER_1_PORT), headers.get("X-Port"));
        assertEquals(List.of(CONTAINER_1_PRIVILEGED_USERNAME), headers.get("X-Username"));
        assertEquals(List.of(CONTAINER_1_PRIVILEGED_PASSWORD), headers.get("X-Password"));
        assertEquals(List.of(IMAGE_1_JDBC_METHOD), headers.get("X-Jdbc-Method"));
        assertEquals(List.of("X-Username X-Password X-Jdbc-Method X-Host X-Port"), headers.get("Access-Control-Expose-Headers"));
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

    public void list_generic(String internalName, Principal principal, Integer expectedSize) {

        /* test */
        final ResponseEntity<List<DatabaseBriefDto>> response = databaseEndpoint.list(internalName, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<DatabaseBriefDto> body = response.getBody();
        assertEquals(expectedSize, body.size());
    }

    public void create_generic(CreateDatabaseDto data, Principal principal, String username)
            throws DataServiceException, DataServiceConnectionException, UserNotFoundException,
            DatabaseNotFoundException, BrokerServiceException, ContainerNotFoundException, SearchServiceException,
            SearchServiceConnectionException, BrokerServiceConnectionException, ContainerQuotaException,
            DashboardServiceException, DashboardServiceConnectionException, NotAllowedException {

        /* mock */
        doNothing()
                .when(messageQueueService)
                .setVirtualHostPermissions(username);
        when(databaseService.findById(any(UUID.class)))
                .thenReturn(DATABASE_1);
        when(dashboardService.create(DATABASE_1))
                .thenReturn(CreateDashboardResponseDto.builder()
                        .uid(DATABASE_1_DASHBOARD_UID)
                        .build());

        /* test */
        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void visibility_generic(UUID databaseId, Database database, DatabaseModifyVisibilityDto data,
                                   Principal principal) throws NotAllowedException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException, DashboardServiceException,
            DashboardServiceConnectionException {

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
        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.visibility(databaseId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public ResponseEntity<DatabaseDto> findById_generic(UUID databaseId, Database database, Principal principal)
            throws DatabaseNotFoundException, NotAllowedException {

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
        return response;
    }

    public ResponseEntity<byte[]> findPreviewImage_generic(UUID databaseId, Database database)
            throws DatabaseNotFoundException {

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
