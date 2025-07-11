package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.config.EndpointConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.ld.LdDatasetDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierType;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierEndpointUnitTest extends BaseTest {

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
                Arguments.arguments("foreign_subset", 2, 5, 1),
                Arguments.arguments("foreign_database", 1, 1, 1),
                Arguments.arguments("foreign_view", 1, 3, 1),
                Arguments.arguments("foreign_table", 1, 4, 1)
        );
    }

    public static Stream<Arguments> malformedDatabase_parameters() {
        return Stream.of(
                Arguments.arguments("queryId", "deadbeef-1f60-4297-9eab-5b0321d96dd7", null, null),
                Arguments.arguments("viewId", null, "deadbeef-1f60-4297-9eab-5b0321d96dd7", null),
                Arguments.arguments("tableId", null, null, "deadbeef-1f60-4297-9eab-5b0321d96dd7")
        );
    }

    public static Stream<Arguments> malformedSubset_parameters() {
        return Stream.of(
                Arguments.arguments("queryId", null, null, null),
                Arguments.arguments("viewId", null, "deadbeef-1f60-4297-9eab-5b0321d96dd7", null),
                Arguments.arguments("tableId", null, null, "deadbeef-1f60-4297-9eab-5b0321d96dd7")
        );
    }

    public static Stream<Arguments> malformedView_parameters() {
        return Stream.of(
                Arguments.arguments("queryId", "deadbeef-1f60-4297-9eab-5b0321d96dd7", null, null),
                Arguments.arguments("viewId", null, null, null),
                Arguments.arguments("tableId", null, null, "deadbeef-1f60-4297-9eab-5b0321d96dd7")
        );
    }

    public static Stream<Arguments> malformedTable_parameters() {
        return Stream.of(
                Arguments.arguments("queryId", "deadbeef-1f60-4297-9eab-5b0321d96dd7", null, null),
                Arguments.arguments("viewId", null, "deadbeef-1f60-4297-9eab-5b0321d96dd7", null),
                Arguments.arguments("tableId", null, null, null)
        );
    }

    public static Stream<Arguments> findAll_anonymousFilterDatabase_parameters() {
        return Stream.of(
                Arguments.arguments("dbid", DATABASE_1_ID, null, null, null, null, 0),
                Arguments.arguments("qid", DATABASE_1_ID, QUERY_1_ID, null, null, null, 0),
                Arguments.arguments("vid", DATABASE_1_ID, null, VIEW_1_ID, null, null, 0),
                Arguments.arguments("tid", DATABASE_1_ID, null, null, TABLE_1_ID, null, 0),
                Arguments.arguments("status_published", DATABASE_1_ID, null, null, null, "PUBLISHED", 0),
                Arguments.arguments("status_draft", DATABASE_1_ID, null, null, null, "DRAFT", 0)
        );
    }

    public static Stream<Arguments> findAll_filterSubset_parameters() {
        return Stream.of(
                Arguments.arguments("status_published", DATABASE_2_ID, null, null, null, "PUBLISHED", 0),
                Arguments.arguments("status_draft", DATABASE_2_ID, null, null, null, "DRAFT", 1)
        );
    }

    public static Stream<Arguments> findAll_filterDatabase_parameters() {
        return Stream.of(
                Arguments.arguments("database_dbid", IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, null, null, null, 1, 1),
                Arguments.arguments("database_qid", IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, QUERY_1_ID, null, null, 0, 1),
                Arguments.arguments("database_vid", IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, null, VIEW_1_ID, null, 0, 1),
                Arguments.arguments("database_tid", IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, null, null, TABLE_1_ID, 0, 1),
                Arguments.arguments("subset_dbid", IdentifierTypeDto.SUBSET, null, DATABASE_1_ID, null, null, null, 1, 1),
                Arguments.arguments("subset_qid", IdentifierTypeDto.SUBSET, null, DATABASE_1_ID, QUERY_1_ID, null, null, 1, 1),
                Arguments.arguments("subset_vid", IdentifierTypeDto.SUBSET, null, DATABASE_1_ID, null, VIEW_1_ID, null, 0, 1),
                Arguments.arguments("subset_tid", IdentifierTypeDto.SUBSET, null, DATABASE_1_ID, null, null, TABLE_1_ID, 0, 1),
                Arguments.arguments("view_dbid", IdentifierTypeDto.VIEW, null, DATABASE_1_ID, null, null, null, 1, 1),
                Arguments.arguments("view_qid", IdentifierTypeDto.VIEW, null, DATABASE_1_ID, QUERY_1_ID, null, null, 0, 1),
                Arguments.arguments("view_vid", IdentifierTypeDto.VIEW, null, DATABASE_1_ID, null, VIEW_1_ID, null, 1, 1),
                Arguments.arguments("view_tid", IdentifierTypeDto.VIEW, null, DATABASE_1_ID, null, null, TABLE_1_ID, 0, 1),
                Arguments.arguments("table_dbid", IdentifierTypeDto.TABLE, null, DATABASE_1_ID, null, null, null, 1, 1),
                Arguments.arguments("table_qid", IdentifierTypeDto.TABLE, null, DATABASE_1_ID, QUERY_1_ID, null, null, 0, 1),
                Arguments.arguments("table_vid", IdentifierTypeDto.TABLE, null, DATABASE_1_ID, null, VIEW_1_ID, null, 0, 1),
                Arguments.arguments("table_tid", IdentifierTypeDto.TABLE, null, DATABASE_1_ID, null, null, TABLE_1_ID, 1, 1),
                Arguments.arguments("anon_database_dbid", IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, null, null, null, 0, null),
                Arguments.arguments("anon_database_qid", IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, QUERY_1_ID, null, null, 0, null),
                Arguments.arguments("anon_database_vid", IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, null, VIEW_1_ID, null, 0, null),
                Arguments.arguments("anon_database_tid", IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, null, null, TABLE_1_ID, 0, null),
                Arguments.arguments("anon_subset_dbid", IdentifierTypeDto.SUBSET, null, DATABASE_1_ID, null, null, null, 0, null),
                Arguments.arguments("anon_subset_qid", IdentifierTypeDto.SUBSET, null, DATABASE_1_ID, QUERY_1_ID, null, null, 0, null),
                Arguments.arguments("anon_subset_vid", IdentifierTypeDto.SUBSET, null, DATABASE_1_ID, null, VIEW_1_ID, null, 0, null),
                Arguments.arguments("anon_subset_tid", IdentifierTypeDto.SUBSET, null, DATABASE_1_ID, null, null, TABLE_1_ID, 0, null),
                Arguments.arguments("anon_view_dbid", IdentifierTypeDto.VIEW, null, DATABASE_1_ID, null, null, null, 0, null),
                Arguments.arguments("anon_view_qid", IdentifierTypeDto.VIEW, null, DATABASE_1_ID, QUERY_1_ID, null, null, 0, null),
                Arguments.arguments("anon_view_vid", IdentifierTypeDto.VIEW, null, DATABASE_1_ID, null, VIEW_1_ID, null, 0, null),
                Arguments.arguments("anon_view_tid", IdentifierTypeDto.VIEW, null, DATABASE_1_ID, null, null, TABLE_1_ID, 0, null),
                Arguments.arguments("anon_table_dbid", IdentifierTypeDto.TABLE, null, DATABASE_1_ID, null, null, null, 1, null),
                Arguments.arguments("anon_table_qid", IdentifierTypeDto.TABLE, null, DATABASE_1_ID, QUERY_1_ID, null, null, 0, null),
                Arguments.arguments("anon_table_vid", IdentifierTypeDto.TABLE, null, DATABASE_1_ID, null, VIEW_1_ID, null, 0, null),
                Arguments.arguments("anon_table_tid", IdentifierTypeDto.TABLE, null, DATABASE_1_ID, null, null, TABLE_1_ID, 1, null)
        );
    }

    public static Stream<Arguments> save_foreign_parameters() {
        return Stream.of(
                Arguments.arguments("view", 3),
                Arguments.arguments("table", 4),
                Arguments.arguments("subset", 2)
        );
    }

    @Test
    @WithAnonymousUser
    public void findAll_empty_succeeds() {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(null, null, null, null, null, null, "application/json", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(0, identifiers.size());
    }

    @ParameterizedTest
    @MethodSource("findAll_anonymousFilterDatabase_parameters")
    @WithAnonymousUser
    public void findAll_anonymousFilterDatabase_succeeds(String name, UUID databaseId, UUID queryId, UUID viewId,
                                                         UUID tableId, IdentifierStatusTypeDto status,
                                                         Integer expectedSize) throws ViewNotFoundException,
            TableNotFoundException, DatabaseNotFoundException {

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
        final ResponseEntity<?> response = identifierEndpoint.findAll(IdentifierTypeDto.DATABASE, status, databaseId, queryId, viewId, tableId, "application/json", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(expectedSize, identifiers.size());
    }

    @ParameterizedTest
    @MethodSource("findAll_filterSubset_parameters")
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_filterSubset_succeeds(String name, UUID databaseId, UUID queryId, UUID viewId, UUID tableId,
                                              IdentifierStatusTypeDto status, Integer expectedSize) {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4, IDENTIFIER_5, IDENTIFIER_6, IDENTIFIER_7));

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(IdentifierTypeDto.SUBSET, status, databaseId, queryId, viewId, tableId, "application/json", USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(expectedSize, identifiers.size());
    }

    @ParameterizedTest
    @MethodSource("findAll_anonymousFilterDatabase_parameters")
    @WithAnonymousUser
    public void findAll_wrongPrincipalFilterDatabase_succeeds(String name, UUID databaseId, UUID queryId, UUID viewId,
                                                              UUID tableId, IdentifierStatusTypeDto status,
                                                              Integer expectedSize)
            throws ViewNotFoundException, TableNotFoundException, DatabaseNotFoundException {

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
        final ResponseEntity<?> response = identifierEndpoint.findAll(IdentifierTypeDto.DATABASE, status, databaseId, queryId, viewId, tableId, "application/json", USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(expectedSize, identifiers.size());
    }

    @ParameterizedTest
    @MethodSource("findAll_filterDatabase_parameters")
    @WithAnonymousUser
    public void findAll_filterDatabase_succeeds(String name, IdentifierTypeDto type, IdentifierStatusTypeDto status,
                                                UUID databaseId, UUID queryId, UUID viewId, UUID tableId,
                                                Integer expectedSize, Integer idx) throws ViewNotFoundException,
            TableNotFoundException, DatabaseNotFoundException {

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
        final Principal principal;
        if (idx == null) {
            principal = null;
        } else {
            principal = USER_1_PRINCIPAL;
        }

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(type, status, databaseId, queryId, viewId, tableId, "application/json", principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(expectedSize, identifiers.size());
    }

    @Test
    @WithAnonymousUser
    public void findAll_json_succeeds() {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_4));

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(null, null, null, null, null, null, "application/json", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(1, identifiers.size());
    }

    @Test
    @WithAnonymousUser
    public void findAll_jsonLd_succeeds() {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_4));

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(null, null, null, null, null, null, "application/ld+json", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<LdDatasetDto> identifiers = (List<LdDatasetDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(1, identifiers.size());
    }

    @Test
    @WithAnonymousUser
    public void findAll_format_succeeds() {

        /* mock */
        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_4));

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.findAll(null, null, null, null, null, null, "text/html", null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<IdentifierBriefDto> identifiers = (List<IdentifierBriefDto>) response.getBody();
        assertNotNull(identifiers);
        assertEquals(1, identifiers.size());
    }

    @Test
    @WithAnonymousUser
    public void find_draft_fails() throws IdentifierNotFoundException {

        /* mock */
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.find(IDENTIFIER_5_ID, "application/json", null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_draftNotOwner_fails() throws IdentifierNotFoundException {

        /* mock */
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.find(IDENTIFIER_5_ID, "application/json", USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_draft_succeeds() throws IdentifierNotFoundException, MalformedException, NotAllowedException,
            DataServiceException, QueryNotFoundException, DataServiceConnectionException, FormatNotAvailableException,
            TableNotFoundException, ViewNotFoundException {

        /* mock */
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        identifierEndpoint.find(IDENTIFIER_5_ID, "application/json", USER_2_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void find_defaultHtmlRespondsJson_succeeds() throws IdentifierNotFoundException, MalformedException,
            NotAllowedException, DataServiceException, QueryNotFoundException, DataServiceConnectionException,
            FormatNotAvailableException, TableNotFoundException, ViewNotFoundException {

        /* mock */
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        identifierEndpoint.find(IDENTIFIER_4_ID, "text/html", null);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void find_json0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata0.json"), StandardCharsets.UTF_8), IdentifierDto.class);

        /* mock */
        when(identifierService.find(IDENTIFIER_7.getId()))
                .thenReturn(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_7.getId(), accept, USER_4_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
        assertEquals(compare.getId(), body.getId());
        assertEquals(compare.getTitles(), body.getTitles());
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
    public void find_json4_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "application/json";
        final IdentifierDto compare = objectMapper.readValue(FileUtils.readFileToString(new File("src/test/resources/json/metadata4.json"), StandardCharsets.UTF_8), IdentifierDto.class);

        /* mock */
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
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
    public void find_bibliography_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_4, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_anonymousBibliographyApa0_fails() throws IOException, MalformedException,
            IdentifierNotFoundException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_7, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_7.getId()))
                .thenReturn(IDENTIFIER_7);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.find(IDENTIFIER_7.getId(), accept, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void find_bibliographyApa0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_7, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_7.getId()))
                .thenReturn(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_7.getId(), accept, USER_4_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa1_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_4, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_draftBibliographyApa2_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa2.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_5, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_5_ID, accept, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa3_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_6, BibliographyTypeDto.APA))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_6_ID))
                .thenReturn(IDENTIFIER_6);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_6_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyApa4_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=apa";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_apa4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(any(Identifier.class), eq(BibliographyTypeDto.APA)))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void find_bibliographyIeee0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_7, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_7.getId()))
                .thenReturn(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_7.getId(), accept, USER_4_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee1_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_4, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_bibliographyIeee2_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee5.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_5, BibliographyTypeDto.IEEE))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_5_ID, accept, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyIeee3_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=ieee";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_ieee3.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(any(Identifier.class), eq(BibliographyTypeDto.IEEE)))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void find_bibliographyBibtex0_succeeds() throws IOException, MalformedException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex0.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_7, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_7.getId()))
                .thenReturn(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_7.getId(), accept, USER_4_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex1_succeeds() throws MalformedException, IOException, DataServiceException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_4, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_bibliographyBibtex2_succeeds() throws MalformedException, DataServiceException, IOException,
            DataServiceConnectionException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex2.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(IDENTIFIER_5, BibliographyTypeDto.BIBTEX))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_5_ID, accept, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_bibliographyBibtex3_succeeds() throws MalformedException, DataServiceException,
            DataServiceConnectionException, IOException, QueryNotFoundException, IdentifierNotFoundException,
            FormatNotAvailableException, NotAllowedException, TableNotFoundException, ViewNotFoundException {
        final String accept = "text/bibliography; style=bibtex";
        final String compare = FileUtils.readFileToString(new File("src/test/resources/bibliography/style_bibtex4.txt"),
                StandardCharsets.UTF_8);

        /* mock */
        when(identifierService.exportBibliography(any(Identifier.class), eq(BibliographyTypeDto.BIBTEX)))
                .thenReturn(compare);
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = (String) response.getBody();
        assertNotNull(body);
        assertEquals(compare, body);
    }

    @Test
    @WithAnonymousUser
    public void find_jsonLd_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            QueryNotFoundException, IdentifierNotFoundException, FormatNotAvailableException, NotAllowedException,
            TableNotFoundException, ViewNotFoundException {
        final String accept = "application/ld+json";

        /* mock */
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final LdDatasetDto body = (LdDatasetDto) response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithAnonymousUser
    public void find_jsonDatabase_fails() throws IdentifierNotFoundException {
        final String accept = "text/csv";

        /* mock */
        when(identifierService.find(IDENTIFIER_7.getId()))
                .thenReturn(IDENTIFIER_7);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.find(IDENTIFIER_7.getId(), accept, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_move_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            QueryNotFoundException, IdentifierNotFoundException, FormatNotAvailableException, NotAllowedException,
            TableNotFoundException, ViewNotFoundException {

        /* mock */
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, null, null);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, this::generic_delete);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
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
        generic_delete();
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-identifier"})
    public void delete_alreadyPublished_fails() throws DatabaseNotFoundException, IdentifierNotFoundException,
            SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);
        doNothing()
                .when(identifierService)
                .delete(IDENTIFIER_4);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.delete(IDENTIFIER_4_ID);
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
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
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
            FormatNotAvailableException, QueryNotFoundException, IdentifierNotFoundException, NotAllowedException,
            TableNotFoundException, ViewNotFoundException {
        final String accept = "application/json";

        /* mock */
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final IdentifierDto body = (IdentifierDto) response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithAnonymousUser
    public void find_xml_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            IOException, QueryNotFoundException, IdentifierNotFoundException, FormatNotAvailableException,
            NotAllowedException, TableNotFoundException, ViewNotFoundException {
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
    public void find_httpRedirect_succeeds() throws MalformedException, DataServiceException, QueryNotFoundException,
            DataServiceConnectionException, FormatNotAvailableException, IdentifierNotFoundException,
            NotAllowedException, TableNotFoundException, ViewNotFoundException {

        /* test */
        final ResponseEntity<?> response = generic_find(null, null);
        assertEquals(HttpStatus.MOVED_PERMANENTLY, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Location"));
        assertEquals(endpointConfig.getWebsiteUrl() + "/database/" + DATABASE_1_ID + "/table/" + TABLE_1_ID + "/info?pid=" + IDENTIFIER_4_ID,
                response.getHeaders().getFirst("Location"));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_succeeds() throws MalformedException, NotAllowedException, DataServiceException,
            DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException, AccessNotFoundException,
            QueryNotFoundException, IdentifierNotFoundException, ViewNotFoundException, SearchServiceException,
            SearchServiceConnectionException, TableNotFoundException, ExternalServiceException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, IDENTIFIER_1_SAVE_DTO, USER_1_PRINCIPAL, USER_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_noAccess_fails() throws IdentifierNotFoundException {

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

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
        when(identifierService.find(IDENTIFIER_5_ID))
                .thenReturn(IDENTIFIER_5);
        when(dataServiceGateway.findQuery(DATABASE_2_ID, QUERY_2_ID))
                .thenReturn(QUERY_2_DTO);

        /* test */
        generic_save(DATABASE_2_ID, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS, IDENTIFIER_5, IDENTIFIER_5_SAVE_DTO, USER_2_PRINCIPAL, USER_2);
    }

    @ParameterizedTest
    @MethodSource("malformedSubset_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_malformedSubset_fails(String name, UUID queryId, UUID viewId, UUID tableId)
            throws IdentifierNotFoundException {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .id(IDENTIFIER_1_ID)
                .queryId(queryId)
                .viewId(viewId)
                .tableId(tableId)
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO))
                .relatedIdentifiers(List.of())
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_SAVE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.SUBSET)
                .licenses(List.of(LICENSE_1_DTO))
                .funders(List.of())
                .build();

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @ParameterizedTest
    @MethodSource("malformedView_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_malformedView_fails(String name, UUID queryId, UUID viewId, UUID tableId)
            throws IdentifierNotFoundException {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .id(IDENTIFIER_1_ID)
                .queryId(queryId)
                .viewId(viewId)
                .tableId(tableId)
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO))
                .relatedIdentifiers(List.of())
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_SAVE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.VIEW)
                .licenses(List.of(LICENSE_1_DTO))
                .funders(List.of())
                .build();

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @ParameterizedTest
    @MethodSource("malformedTable_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_malformedTable_fails(String name, UUID queryId, UUID viewId, UUID tableId)
            throws IdentifierNotFoundException {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .id(IDENTIFIER_1_ID)
                .queryId(queryId)
                .viewId(viewId)
                .tableId(tableId)
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO))
                .relatedIdentifiers(List.of())
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_SAVE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.TABLE)
                .licenses(List.of(LICENSE_1_DTO))
                .funders(List.of())
                .build();

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @ParameterizedTest
    @MethodSource("malformedDatabase_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_malformedDatabase_fails(String name, UUID queryId, UUID viewId, UUID tableId)
            throws IdentifierNotFoundException {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .id(IDENTIFIER_1_ID)
                .queryId(queryId)
                .viewId(viewId)
                .tableId(tableId)
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO))
                .relatedIdentifiers(List.of())
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_SAVE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.DATABASE)
                .funders(List.of())
                .licenses(List.of(LICENSE_1_DTO))
                .build();

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_invalidDateDay_fails() throws IdentifierNotFoundException {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .id(IDENTIFIER_1_ID)
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_SAVE_DTO))
                .publicationDay(32) // <<<
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_SAVE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.DATABASE)
                .licenses(List.of(LICENSE_1_DTO))
                .funders(List.of())
                .build();

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_invalidDateMonth_fails() throws IdentifierNotFoundException {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .id(IDENTIFIER_1_ID)
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_SAVE_DTO))
                .publicationDay(IDENTIFIER_1_PUBLICATION_DAY)
                .publicationMonth(13) // <<<
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_SAVE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.DATABASE)
                .funders(List.of())
                .licenses(List.of(LICENSE_1_DTO))
                .build();

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @ParameterizedTest
    @MethodSource("save_foreign_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_foreign_fails(String name, Integer idx)
            throws UserNotFoundException, IdentifierNotFoundException {

        /* mock */
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);
        final Identifier identifier;
        final IdentifierSaveDto identifierSave = switch (idx) {
            case 2 -> {
                identifier = IDENTIFIER_2;
                yield IDENTIFIER_2_SAVE_DTO;
            }
            case 3 -> {
                identifier = IDENTIFIER_3;
                yield IDENTIFIER_3_SAVE_DTO;
            }
            case 4 -> {
                identifier = IDENTIFIER_4;
                yield IDENTIFIER_4_SAVE_DTO;
            }
            default -> {
                identifier = null;
                yield null;
            }
        };

        /* mock */
        when(identifierService.find(identifier.getId()))
                .thenReturn(identifier);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, null, identifier, identifierSave, USER_1_PRINCIPAL, USER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_invalidTable_fails() throws IdentifierNotFoundException {
        final IdentifierSaveDto request = IdentifierSaveDto.builder()
                .id(IDENTIFIER_1_ID)
                .viewId(UUID.randomUUID())  // <--
                .databaseId(DATABASE_1_ID)
                .descriptions(List.of(IDENTIFIER_1_DESCRIPTION_1_SAVE_DTO))
                .titles(List.of(IDENTIFIER_1_TITLE_1_SAVE_DTO))
                .relatedIdentifiers(List.of(IDENTIFIER_1_RELATED_IDENTIFIER_5_SAVE_DTO))
                .publicationDay(IDENTIFIER_1_PUBLICATION_DAY)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .creators(List.of(IDENTIFIER_1_CREATOR_1_SAVE_DTO))
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IdentifierTypeDto.TABLE)
                .funders(List.of())
                .licenses(List.of(LICENSE_1_DTO))
                .build();

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        assertThrows(MalformedException.class, () -> {
            generic_save(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, IDENTIFIER_1, request, USER_1_PRINCIPAL, USER_1);
        });
    }

    @ParameterizedTest
    @MethodSource("save_parameters")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void save_noForeign_fails(String name, Integer dbIdx, Integer idIdx, Integer uIdx)
            throws IdentifierNotFoundException {
        final UUID databaseId;
        final Database database = switch (dbIdx) {
            case 1 -> {
                databaseId = DATABASE_1_ID;
                yield DATABASE_1;
            }
            case 2 -> {
                databaseId = DATABASE_2_ID;
                yield DATABASE_2;
            }
            default -> {
                databaseId = null;
                yield null;
            }
        };
        final IdentifierSaveDto identifierSave;
        final Identifier identifier = switch (idIdx) {
            case 1 -> {
                identifierSave = IDENTIFIER_1_SAVE_DTO;
                yield IDENTIFIER_1;
            }
            case 3 -> {
                identifierSave = IDENTIFIER_3_SAVE_DTO;
                yield IDENTIFIER_3;
            }
            case 4 -> {
                identifierSave = IDENTIFIER_4_SAVE_DTO;
                yield IDENTIFIER_4;
            }
            case 5 -> {
                identifierSave = IDENTIFIER_5_SAVE_DTO;
                yield IDENTIFIER_5;
            }
            default -> {
                identifierSave = null;
                yield null;
            }
        };
        final Principal principal;
        final User user = switch (uIdx) {
            case 1 -> {
                principal = USER_1_PRINCIPAL;
                yield USER_1;
            }
            default -> {
                principal = null;
                yield null;
            }
        };

        /* mock */
        when(identifierService.find(identifier.getId()))
                .thenReturn(identifier);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_save(databaseId, database, null, identifier, identifierSave, principal, user);
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
        final ResponseEntity<IdentifierDto> response = identifierEndpoint.create(IDENTIFIER_1_CREATE_DTO,
                USER_1_PRINCIPAL);
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
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_validateEmptyDescriptions_fails() {
        final CreateIdentifierDto request = CreateIdentifierDto.builder()
                .databaseId(DATABASE_1_ID)
                .type(IDENTIFIER_1_TYPE_DTO)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .publisher(IDENTIFIER_1_PUBLISHER)
                .descriptions(new LinkedList<>()) // <<<<
                .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO, IDENTIFIER_1_TITLE_2_CREATE_DTO)))
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IDENTIFIER_1_TYPE_DTO)
                .doi(IDENTIFIER_1_DOI)
                .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
                .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO)))
                .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_CREATE_DTO)))
                .relatedIdentifiers(new LinkedList<>())
                .build();

        /* test */
        assertThrows(ConstraintViolationException.class, () -> {
            identifierEndpoint.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_validateEmptyTitles_fails() {
        final CreateIdentifierDto request = CreateIdentifierDto.builder()
                .databaseId(DATABASE_1_ID)
                .type(IDENTIFIER_1_TYPE_DTO)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .publisher(IDENTIFIER_1_PUBLISHER)
                .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO)))
                .titles(new LinkedList<>()) // <<<<
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IDENTIFIER_1_TYPE_DTO)
                .doi(IDENTIFIER_1_DOI)
                .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
                .creators(new LinkedList<>(List.of(IDENTIFIER_1_CREATOR_1_CREATE_DTO)))
                .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_CREATE_DTO)))
                .relatedIdentifiers(new LinkedList<>())
                .build();

        /* test */
        assertThrows(ConstraintViolationException.class, () -> {
            identifierEndpoint.create(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_validateEmptyCreators_fails() {
        final CreateIdentifierDto request = CreateIdentifierDto.builder()
                .databaseId(DATABASE_1_ID)
                .type(IDENTIFIER_1_TYPE_DTO)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .publisher(IDENTIFIER_1_PUBLISHER)
                .descriptions(new LinkedList<>(List.of(IDENTIFIER_1_DESCRIPTION_1_CREATE_DTO)))
                .titles(new LinkedList<>(List.of(IDENTIFIER_1_TITLE_1_CREATE_DTO, IDENTIFIER_1_TITLE_2_CREATE_DTO)))
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .publicationMonth(IDENTIFIER_1_PUBLICATION_MONTH)
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IDENTIFIER_1_TYPE_DTO)
                .doi(IDENTIFIER_1_DOI)
                .licenses(new LinkedList<>(List.of(LICENSE_1_DTO)))
                .creators(new LinkedList<>()) // <<<<
                .funders(new LinkedList<>(List.of(IDENTIFIER_1_FUNDER_1_CREATE_DTO)))
                .relatedIdentifiers(new LinkedList<>())
                .build();

        /* test */
        assertThrows(ConstraintViolationException.class, () -> {
            identifierEndpoint.create(request, USER_1_PRINCIPAL);
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

    protected void generic_save(UUID databaseId, Database database, DatabaseAccess access, Identifier identifier,
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
        when(userService.findByUsername(user.getUsername()))
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
            QueryNotFoundException, IdentifierNotFoundException, NotAllowedException, TableNotFoundException,
            ViewNotFoundException {

        /* mock */
        when(identifierService.find(IDENTIFIER_4_ID))
                .thenReturn(IDENTIFIER_4);
        if (resource != null) {
            when(identifierService.exportMetadata(IDENTIFIER_4))
                    .thenReturn(resource);
        }

        /* test */
        return identifierEndpoint.find(IDENTIFIER_4_ID, accept, null);
    }

    protected void generic_delete() throws NotAllowedException, DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, IdentifierNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        when(identifierService.find(IDENTIFIER_7.getId()))
                .thenReturn(IDENTIFIER_7);
        doNothing()
                .when(identifierService)
                .delete(IDENTIFIER_7);

        /* test */
        final ResponseEntity<?> response = identifierEndpoint.delete(IDENTIFIER_7.getId());
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
