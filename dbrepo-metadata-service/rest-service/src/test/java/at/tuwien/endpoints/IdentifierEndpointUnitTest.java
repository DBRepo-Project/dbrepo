package at.tuwien.endpoints;

import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.identifier.*;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierEndpointUnitTest extends AbstractUnitTest {

    @MockBean
    private IdentifierService identifierService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @MockBean
    private AccessService accessService;

    @MockBean
    private UserService userService;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EndpointConfig endpointConfig;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithAnonymousUser
    public void find_json0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata0.json"), StandardCharsets.UTF_8), IdentifierDto.class);

        /* mock */
        when(identifierService.find(IDENTIFIER_7_ID))
                .thenReturn(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_7_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        assertEquals(compare.getId(), body.getId());
        assertEquals(compare.getTitles().size(), body.getTitles().size());
        assertEquals(compare.getDescriptions().size(), body.getDescriptions().size());
        assertEquals(compare.getDescriptions(), body.getDescriptions());
        assertEquals(compare.getCreated(), body.getCreated());
        assertEquals(compare.getLastModified(), body.getLastModified());
        assertEquals(compare.getDoi(), body.getDoi());
        assertEquals(compare.getLicenses().size(), body.getLicenses().size());
        assertEquals(compare.getPublicationDay(), body.getPublicationDay());
        assertEquals(compare.getPublicationMonth(), body.getPublicationMonth());
        assertEquals(compare.getPublicationYear(), body.getPublicationYear());
        assertEquals(compare.getPublisher(), body.getPublisher());
        assertEquals(compare.getCreators().size(), body.getCreators().size());
    }

    @Test
    @WithAnonymousUser
    public void find_json1_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata1.json"), StandardCharsets.UTF_8), IdentifierDto.class);

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        assertEquals(compare.getId(), body.getId());
        assertEquals(compare.getTitles().size(), body.getTitles().size());
        assertEquals(compare.getTitles().get(0).getId(), body.getTitles().get(0).getId());
        assertEquals(compare.getTitles().get(0).getTitle(), body.getTitles().get(0).getTitle());
        assertEquals(compare.getTitles().get(0).getLanguage(), body.getTitles().get(0).getLanguage());
        assertEquals(compare.getTitles().get(0).getTitleType(), body.getTitles().get(0).getTitleType());
        assertEquals(compare.getDescriptions().size(), body.getDescriptions().size());
        assertEquals(compare.getDescriptions().get(0).getId(), body.getDescriptions().get(0).getId());
        assertEquals(compare.getDescriptions().get(0).getDescription(), body.getDescriptions().get(0).getDescription());
        assertEquals(compare.getDescriptions().get(0).getLanguage(), body.getDescriptions().get(0).getLanguage());
        assertEquals(compare.getDescriptions().get(0).getDescriptionType(), body.getDescriptions().get(0).getDescriptionType());
        assertEquals(compare.getCreated(), body.getCreated());
        assertEquals(compare.getLastModified(), body.getLastModified());
        assertEquals(compare.getDoi(), body.getDoi());
        assertEquals(compare.getLicenses().size(), body.getLicenses().size());
        assertEquals(compare.getLicenses().get(0).getIdentifier(), body.getLicenses().get(0).getIdentifier());
        assertEquals(compare.getLicenses().get(0).getUri(), body.getLicenses().get(0).getUri());
        assertEquals(compare.getPublicationDay(), body.getPublicationDay());
        assertEquals(compare.getPublicationMonth(), body.getPublicationMonth());
        assertEquals(compare.getPublicationYear(), body.getPublicationYear());
        assertEquals(compare.getPublisher(), body.getPublisher());
        assertNotNull(compare.getCreators());
        assertNotNull(body.getCreators());
        assertEquals(compare.getCreators().size(), body.getCreators().size());
        final CreatorDto creator0 = body.getCreators().get(0);
        assertEquals(compare.getCreators().get(0).getFirstname(), creator0.getFirstname());
        assertEquals(compare.getCreators().get(0).getLastname(), creator0.getLastname());
        assertEquals(compare.getCreators().get(0).getCreatorName(), creator0.getCreatorName());
        assertEquals(compare.getCreators().get(0).getAffiliation(), creator0.getAffiliation());
        assertEquals(compare.getCreators().get(0).getAffiliationIdentifier(), creator0.getAffiliationIdentifier());
        assertEquals(compare.getCreators().get(0).getAffiliationIdentifierScheme(), creator0.getAffiliationIdentifierScheme());
        assertEquals(compare.getCreators().get(0).getNameIdentifier(), creator0.getNameIdentifier());
        assertEquals(compare.getCreators().get(0).getNameIdentifierScheme(), creator0.getNameIdentifierScheme());
    }

    @Test
    @WithAnonymousUser
    public void find_csv_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/csv";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/csv/keyboard.csv")));
        final InputStreamResource mock = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/csv/keyboard.csv")));

        /* mock */
        when(identifierService.find(IDENTIFIER_2_ID))
                .thenReturn(IDENTIFIER_2);
        when(identifierService.exportResource(IDENTIFIER_2))
                .thenReturn(mock);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_2_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(compare.getInputStream()), inputStreamToString(body.getInputStream()));
    }

    @Test
    @Disabled("not testable with xml")
    public void find_xml0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, IdentifierNotFoundException, QueryNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/xml";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/xml/metadata0.xml")));

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(compare.getInputStream()), inputStreamToString(body.getInputStream()));
    }

    @Test
    @Disabled("not testable with xml")
    public void find_xml1_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/xml";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/xml/metadata1.xml")));

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(body.getInputStream()), inputStreamToString(compare.getInputStream()));

    }

    @Test
    @WithAnonymousUser
    public void find_bibliography_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_7, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_7_ID))
                .thenReturn(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_7_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa1_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa2_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa2.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_5, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_5_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa3_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_6, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_6_ID))
                .thenReturn(IDENTIFIER_6);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_6_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa4_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1_WITH_DOI);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_7, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_7_ID))
                .thenReturn(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_7_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee1_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee2_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee2.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_5, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_5_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee3_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1_WITH_DOI);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_7, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_7_ID))
                .thenReturn(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_7_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex1_succeeds() throws MalformedException, IOException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex2_succeeds() throws MalformedException, DataServiceException, IOException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex2.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_5, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_5_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex3_succeeds() throws MalformedException, DataServiceException,
            DataServiceConnectionException, IOException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1_WITH_DOI);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, this::generic_delete);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {})
    public void delete_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, this::generic_delete);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-identifier"})
    public void delete_hasRole_succeeds() throws NotAllowedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, IdentifierNotFoundException,
            SearchServiceException, SearchServiceConnectionException {

        /* test */
        this.generic_delete();
    }

    @Test
    @WithAnonymousUser
    public void find_json_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            FormatNotAvailableException, QueryNotFoundException, IdentifierNotFoundException {
        final String accept = "application/json";

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
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
    public void find_xml_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            IOException, QueryNotFoundException, IdentifierNotFoundException, FormatNotAvailableException {
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
    public void find_httpRedirect_succeeds() throws MalformedException, DataServiceException,
            DataServiceConnectionException, FormatNotAvailableException, QueryNotFoundException,
            IdentifierNotFoundException {

        /* test */
        final ResponseEntity<?> response = generic_find(null, null);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Location"));
        assertEquals(endpointConfig.getWebsiteUrl() + "/database/" + IDENTIFIER_1_DATABASE_ID + "/info?pid=" + IDENTIFIER_1_DATABASE_ID,
                response.getHeaders().getFirst("Location"));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_hasRoleDatabase_succeeds() throws MalformedException, NotAllowedException, DataServiceException,
            DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException, AccessNotFoundException,
            QueryNotFoundException, IdentifierNotFoundException, ViewNotFoundException, SearchServiceException,
            SearchServiceConnectionException, TableNotFoundException, ExternalServiceException {

        /* test */
        generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, IDENTIFIER_1_SAVE_DTO, USER_1_PRINCIPAL, USER_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_hasRoleDatabaseNoAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_1, IDENTIFIER_1_SAVE_DTO, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-identifier"})
    public void save_hasRoleReadAccessQuery_succeeds() throws MalformedException, NotAllowedException,
            DataServiceException, DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, QueryNotFoundException, IdentifierNotFoundException, ViewNotFoundException,
            SearchServiceException, SearchServiceConnectionException, TableNotFoundException, ExternalServiceException {

        /* mock */
        when(dataServiceGateway.findQuery(DATABASE_2_ID, IDENTIFIER_5_QUERY_ID))
                .thenReturn(QUERY_2_DTO);

        /* test */
        generic_save(DATABASE_2_ID, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS, IDENTIFIER_5, IDENTIFIER_5_SAVE_DTO, USER_2_PRINCIPAL, USER_2);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_invalidSubset_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .queryId(null)  // <--
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of())
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.SUBSET)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_invalidDatabase_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .queryId(1L) // <--
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
                .publicationDay(IDENTIFIER_1_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.DATABASE)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_invalidView_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .tableId(1L)  // <--
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
                .publicationDay(IDENTIFIER_1_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.VIEW)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_foreignUser_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .viewId(9999L)  // <--
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
                .publicationDay(IDENTIFIER_1_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.VIEW)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_5, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_invalidTable_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .viewId(1L)  // <--
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
                .publicationDay(IDENTIFIER_1_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.TABLE)
                .build();

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_tableNotFound_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .tableId(9999L)  // <--
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
                .publicationDay(IDENTIFIER_1_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.TABLE)
                .build();

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_queryForeign_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_save(DATABASE_2_ID, DATABASE_2, null, IDENTIFIER_5, IDENTIFIER_5_SAVE_DTO, USER_1_PRINCIPAL, USER_1);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_save(Long databaseId, Database database, DatabaseAccess access, Identifier identifier,
                                IdentifierSaveDto data, Principal principal, User user) throws MalformedException,
            NotAllowedException, DataServiceException, DataServiceConnectionException, UserNotFoundException,
            DatabaseNotFoundException, AccessNotFoundException, QueryNotFoundException,
            IdentifierNotFoundException, ViewNotFoundException, SearchServiceException,
            SearchServiceConnectionException, TableNotFoundException, ExternalServiceException {

        /* mock */
        if (access != null) {
            log.trace("mock access: {}", access);
            when(accessService.find(any(Database.class), any(User.class)))
                    .thenReturn(access);
        } else {
            log.trace("mock no access");
            doThrow(AccessNotFoundException.class)
                    .when(accessService)
                    .find(database, user);
        }
        if (identifier.getType().equals(IdentifierType.SUBSET)) {
            when(dataServiceGateway.findQuery(databaseId, QUERY_2_ID))
                    .thenReturn(QUERY_2_DTO);
            when(userService.findById(USER_1_ID))
                    .thenReturn(USER_1);
        }
        when(identifierService.find(identifier.getId()))
                .thenReturn(identifier);
        when(userService.findByUsername(principal.getName()))
                .thenReturn(user);
        when(databaseService.findById(databaseId))
                .thenReturn(database);
        when(identifierService.save(eq(database), eq(user), any(IdentifierSaveDto.class)))
                .thenReturn(identifier);

        /* test */
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.save(identifier.getId(), data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
        assertEquals(identifier.getId(), body.getId());
        assertEquals(identifier.getQuery(), body.getQuery());
        assertEquals(identifier.getQueryHash(), body.getQueryHash());
        assertEquals(identifier.getResultHash(), body.getResultHash());
        assertEquals(identifier.getResultNumber(), body.getResultNumber());
    }

    protected ResponseEntity<?> generic_find(String accept, InputStreamResource resource)
            throws MalformedException, DataServiceException, DataServiceConnectionException, FormatNotAvailableException,
            QueryNotFoundException, IdentifierNotFoundException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        if (resource != null) {
            when(identifierService.exportResource(IDENTIFIER_1))
                    .thenReturn(resource);
            when(identifierService.exportMetadata(IDENTIFIER_1))
                    .thenReturn(resource);
        }

        /* test */
        return identifierEndpoint.find(IDENTIFIER_1_ID, accept);
    }

    protected static String inputStreamToString(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    protected void generic_delete() throws NotAllowedException, DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, IdentifierNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        when(identifierService.find(IDENTIFIER_7_ID))
                .thenReturn(IDENTIFIER_7);
        doNothing()
                .when(identifierService)
                .delete(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.delete(IDENTIFIER_7_ID);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
