package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.api.identifier.CreatorDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.UserService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class PersistenceEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private AccessService accessService;

    @MockBean
    private IdentifierService identifierService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PersistenceEndpoint persistenceEndpoint;

    @Test
    @WithAnonymousUser
    public void find_json0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata0.json"), StandardCharsets.UTF_8), IdentifierDto.class);

        /* mock */
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_4_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        assertEquals(compare.getId(), body.getId());
        assertEquals(compare.getTitles().size(), body.getTitles().size());
        assertEquals(compare.getDescriptions().size(), body.getDescriptions().size());
        assertEquals(compare.getDescriptions(), body.getDescriptions());
        assertEquals(compare.getDatabase().getId(), body.getDatabase().getId());
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
    public void find_json1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata1.json"), StandardCharsets.UTF_8), IdentifierDto.class);

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
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
        assertEquals(compare.getDatabase().getId(), body.getDatabase().getId());
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
    public void find_csv_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/csv";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/csv/keyboard.csv")));
        final InputStreamResource mock = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/csv/keyboard.csv")));

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        when(identifierService.exportResource(IDENTIFIER_1_ID))
                .thenReturn(mock);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(compare.getInputStream()), inputStreamToString(body.getInputStream()));
    }

    @Test
    @Disabled("not testable with xml")
    public void find_xml0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/xml";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/xml/metadata0.xml")));

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(compare.getInputStream()), inputStreamToString(body.getInputStream()));
    }

    @Test
    @Disabled("not testable with xml")
    public void find_xml1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/xml";
        final InputStreamResource compare = new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/xml/metadata1.xml")));

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final InputStreamResource body = (InputStreamResource) response.getBody();
        assertNotNull(body);
        assertEquals(inputStreamToString(body.getInputStream()), inputStreamToString(compare.getInputStream()));

    }

    @Test
    @WithAnonymousUser
    public void find_bibliography_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_4_ID, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_4_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa2_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa2.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_2_ID, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_2_ID))
                .thenReturn(IDENTIFIER_2);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_2_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa3_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_3_ID, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_3_ID))
                .thenReturn(IDENTIFIER_3);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_3_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa4_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1_WITH_DOI);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_4_ID, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_4_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee2_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee2.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_2_ID, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_2_ID))
                .thenReturn(IDENTIFIER_2);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_2_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee3_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1_WITH_DOI);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex0_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_4_ID, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_4_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex1_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex1.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex2_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex2.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_2_ID, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_2_ID))
                .thenReturn(IDENTIFIER_2);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_2_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex3_succeeds() throws IdentifierNotFoundException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierRequestException, IOException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1_WITH_DOI);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void update_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_update(IDENTIFIER_3_ID, IDENTIFIER_3, IDENTIFIER_3_DTO_UPDATE_REQUEST, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME, authorities = {})
    public void update_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_update(IDENTIFIER_3_ID, IDENTIFIER_3, IDENTIFIER_3_DTO_UPDATE_REQUEST, USER_4_USERNAME, USER_4, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"modify-identifier-metadata"})
    public void update_hasRoleNoAccess_succeeds() throws UserNotFoundException, NotAllowedException,
            IdentifierNotFoundException, IdentifierRequestException, QueryNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException {

        /* test */
        generic_update(IDENTIFIER_3_ID, IDENTIFIER_3, IDENTIFIER_3_DTO_UPDATE_REQUEST, USER_3_USERNAME, USER_3, USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"modify-identifier-metadata"})
    public void update_hasRoleHasAccess_succeeds() throws IdentifierNotFoundException, IdentifierRequestException,
            UserNotFoundException, at.tuwien.exception.AccessDeniedException, NotAllowedException,
            QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException {

        /* mock */
        when(accessService.find(IDENTIFIER_3_DATABASE_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS);

        /* test */
        generic_update(IDENTIFIER_3_ID, IDENTIFIER_3, IDENTIFIER_3_DTO_UPDATE_REQUEST, USER_3_USERNAME, USER_3, USER_3_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            this.generic_delete(IDENTIFIER_1_ID, IDENTIFIER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {})
    public void delete_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            this.generic_delete(IDENTIFIER_1_ID, IDENTIFIER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-identifier"})
    public void delete_hasRole_succeeds() throws NotAllowedException, IdentifierNotFoundException {

        /* test */
        this.generic_delete(IDENTIFIER_1_ID, IDENTIFIER_1);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected static String inputStreamToString(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }

    protected void generic_update(Long id, Identifier identifier, IdentifierSaveDto data, String username, User user,
                                  Principal principal) throws IdentifierNotFoundException, IdentifierRequestException,
            UserNotFoundException, NotAllowedException, QueryNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException {

        /* mock */
        if (identifier != null) {
            when(identifierService.update(id, data, principal, "Bearer abc"))
                    .thenReturn(identifier);
            when(identifierService.find(id))
                    .thenReturn(identifier);
        } else {
            doThrow(IdentifierNotFoundException.class)
                    .when(identifierService)
                    .find(id);
        }
        if (user != null) {
            when(userService.findByUsername(username))
                    .thenReturn(user);
        } else {
            doThrow(UserNotFoundException.class)
                    .when(userService)
                    .findByUsername(username);
        }

        /* test */
        final ResponseEntity<IdentifierDto> response = persistenceEndpoint.update(id, data, "Bearer abc", principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
        assertEquals(IDENTIFIER_3_ID, body.getId());
        assertEquals(1, body.getTitles().size());
        assertEquals(IDENTIFIER_3_TITLE_1_TITLE, body.getTitles().get(0).getTitle());
        assertEquals(IDENTIFIER_3_TITLE_1_LANG_DTO, body.getTitles().get(0).getLanguage());
        assertEquals(1, body.getDescriptions().size());
        assertEquals(IDENTIFIER_3_DESCRIPTION_1_DESCRIPTION, body.getDescriptions().get(0).getDescription());
        assertEquals(IDENTIFIER_3_DESCRIPTION_1_LANG_DTO, body.getDescriptions().get(0).getLanguage());
        assertEquals(IDENTIFIER_3_QUERY, body.getQuery());
        assertEquals(IDENTIFIER_3_QUERY_HASH, body.getQueryHash());
        assertEquals(IDENTIFIER_3_RESULT_NUMBER, body.getResultNumber());
        assertEquals(IDENTIFIER_3_RESULT_HASH, body.getResultHash());
    }

    protected void generic_delete(Long id, Identifier identifier) throws IdentifierNotFoundException, NotAllowedException {

        /* mock */
        when(identifierService.find(id))
                .thenReturn(identifier);
        doNothing()
                .when(identifierService)
                .delete(id);

        /* test */
        final ResponseEntity<?> response = persistenceEndpoint.delete(id);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
