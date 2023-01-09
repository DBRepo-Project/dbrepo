package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.EndpointConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.IdentifierEndpoint;
import at.tuwien.endpoints.PersistenceEndpoint;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.mapper.IdentifierMapper;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        assertThrows(UserNotFoundException.class, () -> {
            generic_create(IDENTIFIER_1_DTO_REQUEST, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_creatorResearcherDatabase_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(IDENTIFIER_1_DTO_REQUEST, USER_1_PRINCIPAL, USER_1_USERNAME, USER_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER"})
    public void create_nonCreatorResearcherDatabase_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_create(IDENTIFIER_1_DTO_REQUEST, USER_2_PRINCIPAL, USER_2_USERNAME, USER_2);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_nonCreatorDataStewardDatabase_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(IDENTIFIER_1_DTO_REQUEST, USER_3_PRINCIPAL, USER_3_USERNAME, USER_3);
    }

    @Test
    public void create_anonymousQuery_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            generic_create(IDENTIFIER_2_DTO_REQUEST, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_creatorResearcherQuery_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(IDENTIFIER_2_DTO_REQUEST, USER_1_PRINCIPAL, USER_1_USERNAME, USER_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"RESEARCHER"})
    public void create_nonCreatorResearcherQuery_fails() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(IDENTIFIER_2_DTO_REQUEST, USER_2_PRINCIPAL, USER_2_USERNAME, USER_2);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_nonCreatorDataStewardQuery_succeeds() throws IdentifierAlreadyExistsException,
            UserNotFoundException, QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException,
            IdentifierPublishingNotAllowedException, IdentifierRequestException, NotAllowedException {

        /* test */
        generic_create(IDENTIFIER_2_DTO_REQUEST, USER_3_PRINCIPAL, USER_3_USERNAME, USER_3);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(IdentifierCreateDto identifier, Principal principal, String username, User user)
            throws QueryNotFoundException, RemoteUnavailableException, IdentifierAlreadyExistsException,
            UserNotFoundException, DatabaseNotFoundException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException, NotAllowedException {

        /* mock */
        when(databaseRepository.findByContainerAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseRepository.findByContainerAndDatabaseId(CONTAINER_2_ID, DATABASE_2_ID))
                .thenReturn(Optional.of(DATABASE_2));
        when(identifierRepository.findByDatabaseIdAndQueryId(DATABASE_2_ID, QUERY_2_ID))
                .thenReturn(List.of());
        if (user == null) {
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.empty());
        } else {
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.of(user));
        }
        when(queryServiceGateway.find(CONTAINER_2_ID, DATABASE_2_ID, identifier, "ABC"))
                .thenReturn(QUERY_1_DTO);
        when(identifierRepository.save(any(Identifier.class)))
                .thenReturn(IDENTIFIER_1)
                .thenReturn(IDENTIFIER_1);
        when(relatedIdentifierRepository.save(any(RelatedIdentifier.class)))
                .thenReturn(IDENTIFIER_1_RELATED_IDENTIFIER_1);

        /* test */
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.create(identifier, "ABC", principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
        assertEquals(IDENTIFIER_1_ID, body.getId());
        assertEquals(IDENTIFIER_1_TITLE, body.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, body.getDescription());
        assertEquals(IDENTIFIER_1_QUERY, body.getQuery());
        assertEquals(IDENTIFIER_1_QUERY_HASH, body.getQueryHash());
        assertEquals(IDENTIFIER_1_RESULT_HASH, body.getResultHash());
        assertEquals(IDENTIFIER_1_RESULT_NUMBER, body.getResultNumber());
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
        }

        /* test */
        return persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
    }

}
