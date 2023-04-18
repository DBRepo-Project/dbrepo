package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.CreatorDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.PersistenceEndpoint;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.exception.IdentifierRequestException;
import at.tuwien.exception.QueryNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.repository.jpa.RealmRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PersistenceEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private QueryServiceGateway queryServiceGateway;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersistenceEndpoint persistenceEndpoint;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        return objectMapper;
    }

    @Test
    public void find_json0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata0.json"), StandardCharsets.UTF_8), IdentifierDto.class);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_4_ID))
                .thenReturn(Optional.of(IDENTIFIER_4));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_4_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        assertEquals(compare.getId(), body.getId());
        assertEquals(compare.getTitle(), body.getTitle());
        assertEquals(compare.getDescription(), body.getDescription());
        assertEquals(compare.getContainerId(), body.getContainerId());
        assertEquals(compare.getDatabaseId(), body.getDatabaseId());
        assertEquals(compare.getCreated(), body.getCreated());
        assertEquals(compare.getLastModified(), body.getLastModified());
        assertEquals(compare.getDoi(), body.getDoi());
        assertEquals(compare.getLicense(), body.getLicense());
        assertEquals(compare.getPublicationDay(), body.getPublicationDay());
        assertEquals(compare.getPublicationMonth(), body.getPublicationMonth());
        assertEquals(compare.getPublicationYear(), body.getPublicationYear());
        assertEquals(compare.getPublisher(), body.getPublisher());
        assertEquals(compare.getCreators().size(), body.getCreators().size());
    }

    @Test
    public void find_json1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata1.json"), StandardCharsets.UTF_8), IdentifierDto.class);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        assertEquals(compare.getId(), body.getId());
        assertEquals(compare.getTitle(), body.getTitle());
        assertEquals(compare.getDescription(), body.getDescription());
        assertEquals(compare.getContainerId(), body.getContainerId());
        assertEquals(compare.getDatabaseId(), body.getDatabaseId());
        assertEquals(compare.getCreated(), body.getCreated());
        assertEquals(compare.getLastModified(), body.getLastModified());
        assertEquals(compare.getDoi(), body.getDoi());
        assertEquals(compare.getLicense(), body.getLicense());
        assertEquals(compare.getPublicationDay(), body.getPublicationDay());
        assertEquals(compare.getPublicationMonth(), body.getPublicationMonth());
        assertEquals(compare.getPublicationYear(), body.getPublicationYear());
        assertEquals(compare.getPublisher(), body.getPublisher());
        assertEquals(compare.getCreators().size(), body.getCreators().size());
        final CreatorDto creator1 = body.getCreators().get(0);
        assertEquals(compare.getCreators().get(0).getFirstname(), creator1.getFirstname());
        assertEquals(compare.getCreators().get(0).getLastname(), creator1.getLastname());
        assertEquals(compare.getCreators().get(0).getAffiliation(), creator1.getAffiliation());
        assertEquals(compare.getCreators().get(0).getOrcid(), creator1.getOrcid());
    }

    @Test
    public void find_csv_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/csv";
        final byte[] stream = FileUtils.readFileToByteArray(new File("src/test/resources/csv/keyboard.csv"));
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/csv/keyboard.csv")));

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));
        when(queryServiceGateway.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(stream);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(compare.getInputStream()), inputStreamToString(body.getInputStream()));
    }

    @Test
    @Disabled
    public void find_xml0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/xml";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/xml/metadata0.xml")));

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(compare.getInputStream()), inputStreamToString(body.getInputStream()));
    }

    @Test
    @Disabled
    public void find_xml1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/xml";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/xml/metadata1.xml")));

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(body.getInputStream()), inputStreamToString(compare.getInputStream()));

    }

    @Test
    public void find_bibliography_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
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
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_4_ID))
                .thenReturn(Optional.of(IDENTIFIER_4));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_4_ID, accept);
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
                StandardCharsets.UTF_8);

        /* mock */
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
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_2_ID))
                .thenReturn(Optional.of(IDENTIFIER_2));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_2_ID, accept);
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
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_3_ID))
                .thenReturn(Optional.of(IDENTIFIER_3));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_3_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyApa4_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1_WITH_DOI));

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
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_4_ID))
                .thenReturn(Optional.of(IDENTIFIER_4));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_4_ID, accept);
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
                StandardCharsets.UTF_8);

        /* mock */
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
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_2_ID))
                .thenReturn(Optional.of(IDENTIFIER_2));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_2_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyIeee3_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1_WITH_DOI));

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
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_4_ID))
                .thenReturn(Optional.of(IDENTIFIER_4));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_4_ID, accept);
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
                StandardCharsets.UTF_8);

        /* mock */
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
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_2_ID))
                .thenReturn(Optional.of(IDENTIFIER_2));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_2_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    public void find_bibliographyBibtex3_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1_WITH_DOI));

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    protected static String inputStreamToString(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

}
