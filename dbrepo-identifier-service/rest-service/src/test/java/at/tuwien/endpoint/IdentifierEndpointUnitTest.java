package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.config.EndpointConfig;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.IdentifierEndpoint;
import at.tuwien.endpoints.PersistenceEndpoint;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.repository.jpa.*;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private IdentifierService identifierService;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RelatedIdentifierRepository relatedIdentifierRepository;

    @MockBean
    private AccessRepository accessRepository;

    @MockBean
    private QueryServiceGateway queryServiceGateway;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @Autowired
    private PersistenceEndpoint persistenceEndpoint;

    @Autowired
    private EndpointConfig endpointConfig;

    @Test
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
        assertEquals(IDENTIFIER_1_ID, body.getId());
        assertEquals(IDENTIFIER_1_TITLE, body.getTitle());
    }

    @Test
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
    public void find_httpRedirect_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException {

        /* test */
        final ResponseEntity<?> response = generic_find(null, null);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Location"));
        assertEquals(endpointConfig.getWebsiteUrl() + "/container/" + IDENTIFIER_1_CONTAINER_ID + "/database/"
                + IDENTIFIER_1_DATABASE_ID + "/query/" + IDENTIFIER_1_QUERY_ID, response.getHeaders().getFirst("Location"));
    }

    @Test
    public void create_anonymousDatabase_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, IDENTIFIER_1_DTO_REQUEST, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleDatabase_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, at.tuwien.exception.AccessDeniedException, NotAllowedException {

        /* mock */
        when(accessRepository.findByHdbidAndHuserid(DATABASE_1_ID, USER_1_ID))
                .thenReturn(Optional.of(DATABASE_1_RESEARCHER_READ_ACCESS));

        /* test */
        generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_1_PRINCIPAL, USER_1_USERNAME, USER_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleDatabaseNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_2_PRINCIPAL, USER_2_USERNAME, USER_2);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_anonymousQuery_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_create(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-identifier"})
    public void create_hasRoleReadAccessQuery_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, at.tuwien.exception.AccessDeniedException, NotAllowedException {

        /* mock */
        when(accessRepository.findByHdbidAndHuserid(DATABASE_2_ID, USER_2_ID))
                .thenReturn(Optional.of(DATABASE_2_RESEARCHER_READ_ACCESS));

        /* test */
        generic_create(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, USER_2_PRINCIPAL, USER_2_USERNAME, USER_2);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_invalidSubset_fails() {
        final IdentifierCreateDto request = IdentifierCreateDto.builder()
                .qid(null)  // <--
                .cid(IDENTIFIER_1_CONTAINER_ID)
                .dbid(IDENTIFIER_1_DATABASE_ID)
                .description(IDENTIFIER_1_DESCRIPTION)
                .title(IDENTIFIER_1_TITLE)
                .relatedIdentifiers(List.of())
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(CREATOR_1_CREATE_DTO, CREATOR_1_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.SUBSET)
                .build();

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, request, null, USER_1_PRINCIPAL, USER_1_USERNAME, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_invalidDatabase_fails() {
        final IdentifierCreateDto request = IdentifierCreateDto.builder()
                .qid(IDENTIFIER_1_QUERY_ID) // <--
                .cid(IDENTIFIER_1_CONTAINER_ID)
                .dbid(IDENTIFIER_1_DATABASE_ID)
                .description(IDENTIFIER_1_DESCRIPTION)
                .title(IDENTIFIER_1_TITLE)
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_2_CREATE_DTO))
                .publicationDay(IDENTIFIER_2_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_2_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
                .creators(List.of(CREATOR_1_CREATE_DTO, CREATOR_2_CREATE_DTO))
                .publisher(IDENTIFIER_2_PUBLISHER)
                .type(IdentifierTypeDto.DATABASE)
                .build();

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, request, null, USER_1_PRINCIPAL, USER_1_USERNAME, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_queryForeign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, USER_1_PRINCIPAL, USER_1_USERNAME, USER_1);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(Long containerId, Long databaseId, Database database, IdentifierCreateDto data,
                                  Identifier identifier, Principal principal, String username, User user)
            throws QueryNotFoundException, RemoteUnavailableException,
            IdentifierAlreadyExistsException,
            UserNotFoundException, DatabaseNotFoundException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException, at.tuwien.exception.AccessDeniedException, NotAllowedException {

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
        when(queryServiceGateway.find(containerId, databaseId, data, "ABC"))
                .thenReturn(QUERY_1_DTO);
        when(identifierService.create(data, principal, "ABC"))
                .thenReturn(identifier);
        when(identifierRepository.save(any(Identifier.class)))
                .thenReturn(identifier)
                .thenReturn(identifier);
        when(relatedIdentifierRepository.save(any(RelatedIdentifier.class)))
                .thenReturn(IDENTIFIER_1_RELATED_IDENTIFIER_1);

        /* test */
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.create(data, "ABC", principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
        assertEquals(identifier.getId(), body.getId());
        assertEquals(identifier.getTitle(), body.getTitle());
        assertEquals(identifier.getDescription(), body.getDescription());
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
