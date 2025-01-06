package at.tuwien.endpoints;

import at.tuwien.api.identifier.*;
import at.tuwien.api.identifier.ld.LdDatasetDto;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.service.*;
import at.tuwien.test.AbstractUnitTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.stream.Stream;

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

    @MockBean
    private ViewService viewService;

    @MockBean
    private TableService tableService;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EndpointConfig endpointConfig;

    public static Stream<Arguments> save_parameters() {
        return Stream.of(
                Arguments.arguments("foreign_subset", DATABASE_2_ID, DATABASE_2, null, IDENTIFIER_5, IDENTIFIER_5_SAVE_DTO, USER_1_PRINCIPAL, USER_1),
                Arguments.arguments("foreign_database", DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_1, IDENTIFIER_1_SAVE_DTO, USER_1_PRINCIPAL, USER_1),
                Arguments.arguments("foreign_view", DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_3, IDENTIFIER_3_SAVE_DTO, USER_1_PRINCIPAL, USER_1),
                Arguments.arguments("foreign_table", DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_4, IDENTIFIER_4_SAVE_DTO, USER_1_PRINCIPAL, USER_1)
        );
    }

    public static Stream<Arguments> malformedDatabase_parameters() {
        return Stream.of(
                Arguments.arguments("queryId", 9999L, null, null),
                Arguments.arguments("viewId", null, 9999L, null),
                Arguments.arguments("tableId", null, null, 9999L)
        );
    }

    public static Stream<Arguments> malformedSubset_parameters() {
        return Stream.of(
                Arguments.arguments("queryId", null, null, null),
                Arguments.arguments("viewId", null, 9999L, null),
                Arguments.arguments("tableId", null, null, 9999L)
        );
    }

    public static Stream<Arguments> malformedView_parameters() {
        return Stream.of(
                Arguments.arguments("queryId", 9999L, null, null),
                Arguments.arguments("viewId", null, null, null),
                Arguments.arguments("tableId", null, null, 9999L)
        );
    }

    public static Stream<Arguments> malformedTable_parameters() {
        return Stream.of(
                Arguments.arguments("queryId", 9999L, null, null),
                Arguments.arguments("viewId", null, 9999L, null),
                Arguments.arguments("tableId", null, null, null)
        );
    }

    public static Stream<Arguments> findAll_filterDatabase_parameters() {
        return Stream.of(
                Arguments.arguments("dbid", DATABASE_1_ID, null, null, null, 4),
                Arguments.arguments("qid", DATABASE_1_ID, QUERY_1_ID, null, null, 1),
                Arguments.arguments("vid", DATABASE_1_ID, null, VIEW_1_ID, null, 1),
                Arguments.arguments("tid", DATABASE_1_ID, null, null, TABLE_1_ID, 1)
        );
    }

    public static Stream<Arguments> save_foreign_parameters() {
        return Stream.of(
                Arguments.arguments("view", IDENTIFIER_3, IDENTIFIER_3_SAVE_DTO),
                Arguments.arguments("table", IDENTIFIER_4, IDENTIFIER_4_SAVE_DTO),
                Arguments.arguments("subset", IDENTIFIER_2, IDENTIFIER_2_SAVE_DTO)
        );
    }

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithAnonymousUser
    public void findAll_empty_succeeds() throws FormatNotAvailableException {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(null, null, null, null, "application/json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(0, identifiers.size());
    }

    @ParameterizedTest
    @MethodSource("findAll_filterDatabase_parameters")
    @WithAnonymousUser
    public void findAll_filterDatabase_succeeds(String name, Long databaseId, Long queryId, Long viewId, Long tableId,
                                                Integer expectedSize) throws FormatNotAvailableException,
            ViewNotFoundException, TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4, IDENTIFIER_5, IDENTIFIER_6, IDENTIFIER_7));
        if (viewId != null) {
            when(viewService.findById(DATABASE_1, VIEW_1_ID))
                    .thenReturn(VIEW_1);
        }
        if (tableId != null) {
            when(tableService.findById(DATABASE_1, TABLE_1_ID))
                    .thenReturn(TABLE_1);
        }

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(databaseId, queryId, viewId, tableId, "application/json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(expectedSize, identifiers.size());
    }

    @Test
    @WithAnonymousUser
    public void findAll_json_succeeds() throws FormatNotAvailableException {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(null, null, null, null, "application/json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(1, identifiers.size());
    }

    @Test
    @WithAnonymousUser
    public void findAll_jsonLd_succeeds() throws FormatNotAvailableException {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(null, null, null, null, "application/ld+json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<LdDatasetDto> identifiers = (List<LdDatasetDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(1, identifiers.size());
    }

    @Test
    @WithAnonymousUser
    public void findAll_format_fails() {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        assertThrows(FormatNotAvailableException.class, () -> {
            identifierEndpoint.findAll(null, null, null, null, "text/csv");
        });
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
        when(identifierService.exportBibliography(any(Identifier.class), eq(BibliographyTypeDto.APA)))
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
        when(identifierService.exportBibliography(any(Identifier.class), eq(BibliographyTypeDto.IEEE)))
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
        when(identifierService.exportBibliography(any(Identifier.class), eq(BibliographyTypeDto.BIBTEX)))
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
    public void find_jsonLd_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            QueryNotFoundException, IdentifierNotFoundException, FormatNotAvailableException {
        final String accept = "application/ld+json";

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, accept);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final LdDatasetDto body = (LdDatasetDto) response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithAnonymousUser
    public void find_jsonDatabase_fails() throws IdentifierNotFoundException {
        final String accept = "text/csv";

        /* mock */
        when(identifierService.find(IDENTIFIER_7_ID))
                .thenReturn(IDENTIFIER_7);

        /* test */
        assertThrows(FormatNotAvailableException.class, () -> {
            identifierEndpoint.find(IDENTIFIER_7_ID, accept);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_move_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            QueryNotFoundException, IdentifierNotFoundException, FormatNotAvailableException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_1_ID, null);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
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
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-identifier"})
    public void delete_alreadyPublished_fails() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, IdentifierNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        doNothing()
                .when(identifierService)
                .delete(IDENTIFIER_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.delete(IDENTIFIER_1_ID);
        });
    }

    @Test
    @WithAnonymousUser
    public void publish_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            identifierEndpoint.publish(IDENTIFIER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void publish_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            identifierEndpoint.publish(IDENTIFIER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"publish-identifier"})
    public void publish_succeeds() throws IdentifierNotFoundException, SearchServiceException, MalformedException,
            DatabaseNotFoundException, ExternalServiceException, SearchServiceConnectionException,
            DataServiceConnectionException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        when(identifierService.publish(IDENTIFIER_1))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.publish(IDENTIFIER_1_ID);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"publish-identifier"})
    public void publish_searchService_fails() throws IdentifierNotFoundException, SearchServiceException, MalformedException,
            DatabaseNotFoundException, ExternalServiceException, SearchServiceConnectionException,
            DataServiceConnectionException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        doThrow(SearchServiceException.class)
                .when(identifierService)
                .publish(IDENTIFIER_1);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            identifierEndpoint.publish(IDENTIFIER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"publish-identifier"})
    public void publish_searchServiceConnection_fails() throws IdentifierNotFoundException, SearchServiceException,
            MalformedException, DatabaseNotFoundException, ExternalServiceException, SearchServiceConnectionException,
            DataServiceConnectionException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        doThrow(SearchServiceConnectionException.class)
                .when(identifierService)
                .publish(IDENTIFIER_1);

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            identifierEndpoint.publish(IDENTIFIER_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"publish-identifier"})
    public void publish_notFound_fails() throws IdentifierNotFoundException {

        /* mock */
        doThrow(IdentifierNotFoundException.class)
                .when(identifierService)
                .find(IDENTIFIER_1_ID);

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierEndpoint.publish(IDENTIFIER_1_ID);
        });
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
    public void save_succeeds() throws MalformedException, NotAllowedException, DataServiceException,
            DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException, AccessNotFoundException,
            QueryNotFoundException, IdentifierNotFoundException, ViewNotFoundException, SearchServiceException,
            SearchServiceConnectionException, TableNotFoundException, ExternalServiceException {

        /* test */
        generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, IDENTIFIER_1_SAVE_DTO, USER_1_PRINCIPAL, USER_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_noAccess_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, null, IDENTIFIER_1, IDENTIFIER_1_SAVE_DTO, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-identifier"})
    public void save_readAccessQuery_succeeds() throws MalformedException, NotAllowedException,
            DataServiceException, DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            AccessNotFoundException, QueryNotFoundException, IdentifierNotFoundException, ViewNotFoundException,
            SearchServiceException, SearchServiceConnectionException, TableNotFoundException, ExternalServiceException {

        /* mock */
        when(dataServiceGateway.findQuery(DATABASE_2_ID, IDENTIFIER_5_QUERY_ID))
                .thenReturn(QUERY_2_DTO);

        /* test */
        generic_save(DATABASE_2_ID, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS, IDENTIFIER_5, IDENTIFIER_5_SAVE_DTO, USER_2_PRINCIPAL, USER_2);
    }

    @ParameterizedTest
    @MethodSource("malformedSubset_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_malformedSubset_fails(String name, Long queryId, Long viewId, Long tableId) {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .queryId(queryId)
                .viewId(viewId)
                .tableId(tableId)
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

    @ParameterizedTest
    @MethodSource("malformedView_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_malformedView_fails(String name, Long queryId, Long viewId, Long tableId) {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .queryId(queryId)
                .viewId(viewId)
                .tableId(tableId)
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of())
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

    @ParameterizedTest
    @MethodSource("malformedTable_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_malformedTable_fails(String name, Long queryId, Long viewId, Long tableId) {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .queryId(queryId)
                .viewId(viewId)
                .tableId(tableId)
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of())
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

    @ParameterizedTest
    @MethodSource("malformedDatabase_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_malformedDatabase_fails(String name, Long queryId, Long viewId, Long tableId) {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .queryId(queryId)
                .viewId(viewId)
                .tableId(tableId)
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of())
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
    public void save_invalidDateDay_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
                .publicationDay(32) // <<<
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
    public void save_invalidDateMonth_fails() {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .databaseId(IDENTIFIER_1_DATABASE_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_CREATE_DTO))
                .publicationDay(IDENTIFIER_1_PUBLICATION_DAY)
                .publicationMonth(13) // <<<
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

    @ParameterizedTest
    @MethodSource("save_foreign_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_foreign_fails(String name, Identifier identifier, IdentifierSaveDto data)
            throws UserNotFoundException {

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, null, identifier, data, USER_1_PRINCIPAL, USER_1);
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

    @ParameterizedTest
    @MethodSource("save_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_noForeign_fails(String name, Long databaseId, Database database, DatabaseAccess access,
                                     Identifier identifier, IdentifierSaveDto data, Principal principal, User user) {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_save(databaseId, database, access, identifier, data, principal, user);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_1_CREATE_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_1_CREATE_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_succeeds() throws DatabaseNotFoundException, UserNotFoundException, AccessNotFoundException,
            SearchServiceException, MalformedException, DataServiceException, QueryNotFoundException,
            ExternalServiceException, SearchServiceConnectionException, DataServiceConnectionException,
            IdentifierNotFoundException, ViewNotFoundException, NotAllowedException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        when(accessService.find(DATABASE_1, USER_1))
                .thenReturn(DATABASE_1_USER_1_READ_ACCESS);
        when(identifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_DTO))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.create(IDENTIFIER_1_CREATE_DTO, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final IdentifierDto body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_noAccess_fails() throws DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException {

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        doThrow(AccessNotFoundException.class)
                .when(accessService)
                .find(DATABASE_1, USER_1);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_1_CREATE_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-foreign-identifier"})
    public void create_hasForeign_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            AccessNotFoundException, SearchServiceException, MalformedException, NotAllowedException,
            DataServiceException, QueryNotFoundException, ExternalServiceException, SearchServiceConnectionException,
            DataServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_2_DETAILS, USER_2_PASSWORD, List.of(
                new SimpleGrantedAuthority("create-foreign-identifier")));

        /* mock */
        when(databaseService.findById(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(userService.findByUsername(USER_2_USERNAME))
                .thenReturn(USER_2);
        doThrow(AccessNotFoundException.class)
                .when(accessService)
                .find(DATABASE_1, USER_2);

        /* test */
        identifierEndpoint.create(IDENTIFIER_1_CREATE_DTO, principal);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_databaseNotFound_fails() throws DatabaseNotFoundException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(databaseService)
                .findById(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_1_CREATE_DTO, USER_1_PRINCIPAL);
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
        if (database != null) {
            when(databaseService.findById(databaseId))
                    .thenReturn(database);
        } else {
            doThrow(DatabaseNotFoundException.class)
                    .when(databaseService)
                    .findById(databaseId);
        }
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
