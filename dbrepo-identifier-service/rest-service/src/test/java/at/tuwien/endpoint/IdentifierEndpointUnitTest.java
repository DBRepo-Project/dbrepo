package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.*;
import at.tuwien.config.EndpointConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.endpoints.IdentifierEndpoint;
import at.tuwien.endpoints.PersistenceEndpoint;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.IdentifierService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
public class IdentifierEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private IdentifierService identifierService;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private AccessService accessService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AccessRepository accessRepository;

    @MockBean
    private QueryServiceGateway queryServiceGateway;

    @MockBean
    private IdentifierIdxRepository identifierIdxRepository;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @Autowired
    private PersistenceEndpoint persistenceEndpoint;

    @Autowired
    private EndpointConfig endpointConfig;

    @Test
    @WithAnonymousUser
    public void find_json_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException {
        final String accept = "application/json";

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        final List<IdentifierTitleDto> titles = body.getTitles();
        assertEquals(1, titles.size());
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
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final InputStreamResource resource = new InputStreamResource(FileUtils.openInputStream(
                new File("src/test/resources/xml/datacite-example-dataset-v4.xml")));

        /* test */
        final ResponseEntity<?> response = generic_find("text/xml", resource);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertTrue(body.exists());
        assertEquals(resource, body);
    }

    @Test
    @WithAnonymousUser
    public void find_csv_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IOException, IdentifierRequestException {
        final InputStreamResource resource = new InputStreamResource(FileUtils.openInputStream(
                new File("src/test/resources/csv/testdata.csv")));

        /* test */
        final ResponseEntity<?> response = generic_find("text/csv", resource);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertTrue(body.exists());
        assertEquals(resource, body);
    }

    @Test
    @WithAnonymousUser
    public void find_httpRedirect_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException {

        /* test */
        final ResponseEntity<?> response = generic_find(null, null);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Location"));
        assertEquals(endpointConfig.getWebsiteUrl() + "/database/"
                + IDENTIFIER_1_DATABASE_ID, response.getHeaders().getFirst("Location"));
    }

    @Test
    @WithAnonymousUser
    public void create_anonymousDatabase_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_1_DTO_REQUEST, null, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleDatabase_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException, at.tuwien.exception.AccessDeniedException {

        /* mock */
        when(accessRepository.findByHdbidAndHuserid(DATABASE_1_ID, USER_1_ID))
                .thenReturn(Optional.of(DATABASE_1_USER_1_READ_ACCESS));

        /* test */
        generic_create(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_1_PRINCIPAL, USER_1_ID, USER_1_USERNAME, USER_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleDatabaseNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_1_PRINCIPAL, USER_1_ID, USER_1_USERNAME, USER_1);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_anonymousQuery_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_create(DATABASE_2_ID, DATABASE_2, null, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleReadAccessQuery_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException,
            at.tuwien.exception.AccessDeniedException {

        /* test */
        generic_create(DATABASE_2_ID, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, USER_2_PRINCIPAL, USER_2_ID, USER_2_USERNAME, USER_2);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_invalidSubset_fails() {
        final IdentifierCreateDto request = IdentifierCreateDto.builder()
                .qid(null)  // <--
                .dbid(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of())
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_2_CREATOR_1_CREATE_DTO, IDENTIFIER_2_CREATOR_2_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.SUBSET)
                .build();

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, request, null, USER_1_PRINCIPAL, USER_1_ID, USER_1_USERNAME, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_invalidDatabase_fails() {
        final IdentifierCreateDto request = IdentifierCreateDto.builder()
                .qid(IDENTIFIER_1_QUERY_ID) // <--
                .dbid(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_2_CREATE_DTO))
                .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_2_CREATOR_1_CREATE_DTO, IDENTIFIER_2_CREATOR_2_CREATE_DTO))
                .publisher(IDENTIFIER_2_PUBLISHER)
                .type(IdentifierTypeDto.DATABASE)
                .build();

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, request, null, USER_1_PRINCIPAL, USER_1_ID, USER_1_USERNAME, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_queryForeign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(DATABASE_2_ID, DATABASE_2, null, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, USER_1_PRINCIPAL, USER_1_ID, USER_1_USERNAME, USER_1);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(Long databaseId, Database database, DatabaseAccess access,
                                  IdentifierCreateDto data, Identifier identifier, Principal principal, UUID userId,
                                  String username, User user) throws QueryNotFoundException, RemoteUnavailableException,
            IdentifierAlreadyExistsException, UserNotFoundException, DatabaseNotFoundException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException,
            at.tuwien.exception.AccessDeniedException {

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (user == null) {
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.empty());
        } else {
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.of(user));
        }
        if (access != null) {
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            doThrow(at.tuwien.exception.AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }
        when(queryServiceGateway.find(databaseId, data, "ABC"))
                .thenReturn(QUERY_1_DTO);
        when(identifierService.create(data, principal, "ABC"))
                .thenReturn(identifier);
        when(identifierRepository.save(any(Identifier.class)))
                .thenReturn(identifier)
                .thenReturn(identifier);

        /* test */
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.create(data, "ABC", principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
        assertEquals(identifier.getId(), body.getId());
        assertEquals(identifier.getQuery(), body.getQuery());
        assertEquals(identifier.getQueryHash(), body.getQueryHash());
        assertEquals(identifier.getResultHash(), body.getResultHash());
        assertEquals(identifier.getResultNumber(), body.getResultNumber());
    }

    protected ResponseEntity<?> generic_find(String accept, InputStreamResource resource)
            throws IdentifierNotFoundException, QueryNotFoundException, RemoteUnavailableException,
            IdentifierRequestException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        if (resource != null) {
            when(identifierService.exportResource(IDENTIFIER_1_ID))
                    .thenReturn(resource);
            when(identifierService.exportMetadata(IDENTIFIER_1_ID))
                    .thenReturn(resource);
        }

        /* test */
        return persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
    }

}
