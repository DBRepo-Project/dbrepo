package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.EndpointConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.IdentifierEndpoint;
import at.tuwien.endpoints.PersistenceEndpoint;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.repository.jpa.RelatedIdentifierRepository;
import at.tuwien.repository.jpa.UserRepository;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

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
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_creatorResearcherDatabase_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_1_PRINCIPAL, USER_1_USERNAME, USER_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER"})
    public void create_nonCreatorResearcherDatabase_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_2_PRINCIPAL, USER_2_USERNAME, USER_2);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_nonCreatorDataStewardDatabase_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, IDENTIFIER_1_DTO_REQUEST, IDENTIFIER_1, USER_3_PRINCIPAL, USER_3_USERNAME, USER_3);
    }

    @Test
    public void create_anonymousQuery_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            generic_create(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER"})
    public void create_creatorResearcherQuery_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, USER_2_PRINCIPAL, USER_2_USERNAME, USER_2);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_nonCreatorResearcherQuery_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, USER_1_PRINCIPAL, USER_1_USERNAME, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_nonCreatorDataStewardQuery_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, IDENTIFIER_2_DTO_REQUEST, IDENTIFIER_2, USER_3_PRINCIPAL, USER_3_USERNAME, USER_3);
    }

    @Test
    public void update_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, this::generic_update);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void update_creatorResearcher_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, this::generic_update);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER"})
    public void update_nonCreatorResearcher_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, this::generic_update);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void update_dataSteward_succeeds() throws IdentifierPublishingNotAllowedException,
            IdentifierNotFoundException {

        /* test */
        generic_update();
    }

    @Test
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, this::generic_delete);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_creatorResearcher_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, this::generic_delete);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER"})
    public void delete_nonCreatorResearcher_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, this::generic_delete);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_dataSteward_succeeds() throws IdentifierNotFoundException {

        /* test */
        generic_delete();
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(Long containerId, Long databaseId, Database database, IdentifierCreateDto data,
                                  Identifier identifier, Principal principal, String username, User user)
            throws QueryNotFoundException, RemoteUnavailableException,
            IdentifierAlreadyExistsException,
            UserNotFoundException, DatabaseNotFoundException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException, NotAllowedException {

        /* mock */
        when(databaseRepository.findByContainerAndDatabaseId(containerId, databaseId))
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

    protected void generic_update() throws IdentifierPublishingNotAllowedException, IdentifierNotFoundException {

        /* mock */
        when(identifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO))
                .thenReturn(IDENTIFIER_1);
        when(identifierRepository.save(IDENTIFIER_1))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
        assertEquals(IDENTIFIER_1_ID, body.getId());
        assertEquals(IDENTIFIER_1_TITLE, body.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, body.getDescription());
        assertEquals(IDENTIFIER_1_QUERY, body.getQuery());
        assertEquals(IDENTIFIER_1_QUERY_HASH, body.getQueryHash());
        assertEquals(IDENTIFIER_1_RESULT_NUMBER, body.getResultNumber());
        assertEquals(IDENTIFIER_1_RESULT_HASH, body.getResultHash());
    }

    protected void generic_delete() throws IdentifierNotFoundException {

        /* mock */
        doNothing()
                .when(identifierService)
                .delete(IDENTIFIER_1_ID);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.delete(IDENTIFIER_1_ID);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
