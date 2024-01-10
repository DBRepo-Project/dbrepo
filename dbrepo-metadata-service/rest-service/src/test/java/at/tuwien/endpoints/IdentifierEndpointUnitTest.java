package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.identifier.*;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.mdb.IdentifierRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.StoreService;
import at.tuwien.service.UserService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class IdentifierEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private IdentifierService identifierService;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private AccessService accessService;

    @MockBean
    private StoreService storeService;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @Autowired
    private PersistenceEndpoint persistenceEndpoint;

    @Autowired
    private EndpointConfig endpointConfig;

    @BeforeEach
    public void beforeEach() {
        IDENTIFIER_1.setDatabase(DATABASE_1);
    }

    @Test
    @WithAnonymousUser
    public void find_json_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, FileStorageException, DataDbSidecarException {
        final String accept = "application/json";

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        final List<IdentifierTitleDto> titles = body.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitleDto title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG_DTO, title0.getLanguage());
        final List<IdentifierDescriptionDto> descriptions = body.getDescriptions();
        assertEquals(1, descriptions.size());
        final IdentifierDescriptionDto description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG_DTO, description0.getLanguage());
    }

    @Test
    @WithAnonymousUser
    public void find_xml_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException, UserNotFoundException,
            QueryStoreException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, FileStorageException {
        final InputStreamResource resource = new InputStreamResource(FileUtils.openInputStream(
                new File("src/test/resources/xml/datacite-example-dataset-v4.xml")));

        /* test */
        final ResponseEntity<?> response = generic_find("text/xml", resource, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertTrue(body.exists());
        assertEquals(resource, body);
    }

    @Test
    @WithAnonymousUser
    public void find_csv_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IOException, IdentifierRequestException, UserNotFoundException,
            QueryStoreException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, FileStorageException {
        final InputStreamResource resource = new InputStreamResource(FileUtils.openInputStream(
                new File("src/test/resources/csv/testdata.csv")));

        /* test */
        final ResponseEntity<?> response = generic_find("text/csv", resource, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertTrue(body.exists());
        assertEquals(resource, body);
    }

    @Test
    @WithAnonymousUser
    public void find_httpRedirect_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, FileStorageException, DataDbSidecarException {

        /* test */
        final ResponseEntity<?> response = generic_find(null, null, null);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Location"));
        assertEquals(endpointConfig.getWebsiteUrl() + "/database/" + IDENTIFIER_1_DATABASE_ID + "/info?pid=" + IDENTIFIER_1_DATABASE_ID,
                response.getHeaders().getFirst("Location"));
    }

    @Test
    @WithAnonymousUser
    public void create_anonymousDatabase_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_1_DTO_REQUEST, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleDatabase_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException,
            ViewNotFoundException, at.tuwien.exception.AccessDeniedException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException, IdentifierNotFoundException,
            TableNotFoundException, TableMalformedException, QueryMalformedException, FileStorageException,
            DataDbSidecarException {

        /* test */
        generic_create(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_1_PRINCIPAL, USER_1_ID);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleDatabaseNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_1_PRINCIPAL, USER_1_ID);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_anonymousQuery_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_2_ID, DATABASE_2, null, IDENTIFIER_5_DTO_REQUEST, IDENTIFIER_5, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleReadAccessQuery_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException,
            at.tuwien.exception.AccessDeniedException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException, IdentifierNotFoundException,
            TableNotFoundException, TableMalformedException, QueryMalformedException, FileStorageException,
            DataDbSidecarException {

        /* test */
        generic_create(DATABASE_2_ID, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS, IDENTIFIER_5_DTO_REQUEST, IDENTIFIER_5, USER_2_PRINCIPAL, USER_2_ID);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_invalidSubset_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .queryId(null)  // <--
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of())
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_5_CREATOR_1_CREATE_DTO, IDENTIFIER_5_CREATOR_2_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.SUBSET)
                .build();

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, request, null, USER_1_PRINCIPAL, USER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_invalidDatabase_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .queryId(1L) // <--
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
                .publicationDay(IDENTIFIER_5_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_5_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_5_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_5_CREATOR_1_CREATE_DTO, IDENTIFIER_5_CREATOR_2_CREATE_DTO))
                .publisher(IDENTIFIER_5_PUBLISHER)
                .type(IdentifierTypeDto.DATABASE)
                .build();

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, request, null, USER_1_PRINCIPAL, USER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_queryForeign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(DATABASE_2_ID, DATABASE_2, null, IDENTIFIER_5_DTO_REQUEST, IDENTIFIER_5, USER_1_PRINCIPAL, USER_1_ID);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(Long databaseId, Database database, DatabaseAccess access,
                                  IdentifierSaveDto data, Identifier identifier, Principal principal, UUID userId)
            throws QueryNotFoundException, RemoteUnavailableException, IdentifierAlreadyExistsException,
            UserNotFoundException, DatabaseNotFoundException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException, NotAllowedException, at.tuwien.exception.AccessDeniedException,
            ViewNotFoundException, QueryStoreException, DatabaseConnectionException, ImageNotSupportedException,
            IdentifierNotFoundException, TableNotFoundException, TableMalformedException, QueryMalformedException, FileStorageException, DataDbSidecarException {

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (access != null) {
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            doThrow(at.tuwien.exception.AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        when(storeService.findOne(databaseId, data.getQueryId(), principal))
                .thenReturn(QUERY_1);
        when(identifierService.create(data, principal))
                .thenReturn(identifier);

        /* test */
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
        assertEquals(identifier.getId(), body.getId());
        assertEquals(identifier.getQuery(), body.getQuery());
        assertEquals(identifier.getQueryHash(), body.getQueryHash());
        assertEquals(identifier.getResultHash(), body.getResultHash());
        assertEquals(identifier.getResultNumber(), body.getResultNumber());
    }

    protected ResponseEntity<?> generic_find(String accept, InputStreamResource resource, Principal principal)
            throws IdentifierNotFoundException, QueryNotFoundException, RemoteUnavailableException,
            IdentifierRequestException, UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            FileStorageException, DataDbSidecarException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        if (resource != null) {
            when(identifierService.exportResource(IDENTIFIER_1_ID, principal))
                    .thenReturn(resource);
            when(identifierService.exportMetadata(IDENTIFIER_1_ID))
                    .thenReturn(resource);
        }

        /* test */
        return persistenceEndpoint.find(IDENTIFIER_1_ID, accept, principal);
    }

}
