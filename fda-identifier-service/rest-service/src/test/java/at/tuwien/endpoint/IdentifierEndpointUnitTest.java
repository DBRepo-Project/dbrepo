package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.EndpointConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.PersistenceEndpoint;
import at.tuwien.exception.*;
import at.tuwien.mapper.IdentifierMapper;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IdentifierService identifierService;

    @Autowired
    private IdentifierMapper identifierMapper;

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
        final String accept = "text/xml";
        final InputStreamResource resource = new InputStreamResource(FileUtils.openInputStream(
                new File("src/test/resources/xml/datacite-example-dataset-v4.xml")));

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        when(identifierService.exportMetadata(IDENTIFIER_1_ID))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertTrue(body.exists());
        assertEquals(resource, body);
    }

    @Test
    public void find_csv_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IOException, IdentifierRequestException {
        final String accept = "text/csv";
        final InputStreamResource resource = new InputStreamResource(FileUtils.openInputStream(
                new File("src/test/resources/csv/testdata.csv")));

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        when(identifierService.exportResource(IDENTIFIER_1_ID))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertTrue(body.exists());
        assertEquals(resource, body);
    }

    @Test
    public void find_httpRedirect_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException {
        final String accept = null;

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Location"));
        assertEquals(endpointConfig.getWebsiteUrl() + "/container/" + IDENTIFIER_1_CONTAINER_ID + "/database/" + IDENTIFIER_1_DATABASE_ID + "/query/" + IDENTIFIER_1_QUERY_ID, response.getHeaders().getFirst("Location"));
    }

}
