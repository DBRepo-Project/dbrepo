package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.PersistenceEndpoint;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.exception.IdentifierRequestException;
import at.tuwien.exception.QueryNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.repository.jpa.IdentifierRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PersistenceEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IdentifierRepository identifierRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersistenceEndpoint persistenceEndpoint;

    @Test
    public void find_json0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata0.json")), IdentifierDto.class);

        /* mock */
        IDENTIFIER_1.setCreators(List.of());
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_json1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata1.json"), Charset.defaultCharset()), IdentifierDto.class);

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_csv_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/csv";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/csv/keyboard.csv")));

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_xml0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/xml";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/xml/metadata0.xml")));

        /* mock */
        IDENTIFIER_1.setCreators(List.of());
        IDENTIFIER_1.setCreated(IDENTIFIER_1_CREATED);
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
    }

    @Test
    public void find_xml1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/xml";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/xml/metadata1.xml")));

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1));
        IDENTIFIER_1.setCreated(IDENTIFIER_1_CREATED);
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
    }

    @Test
    public void find_bibliography_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa1.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyApa0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa0.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of());
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyApa1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa1.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyApa2_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa2.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1, CREATOR_2));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyApa3_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa3.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1, CREATOR_2, CREATOR_3));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyIeee0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee0.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of());
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyIeee1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee1.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyIeee2_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee2.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1, CREATOR_2));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyBibtex0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex0.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of());
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyBibtex1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex1.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyBibtex2_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex2.txt"),
                Charset.defaultCharset());

        /* mock */
        IDENTIFIER_1.setCreators(List.of(CREATOR_1, CREATOR_2));
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

}
