package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.entities.identifier.NameIdentifierSchemeType;
import at.tuwien.exception.*;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.repository.mdb.IdentifierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierServiceUnitTest extends BaseUnitTest {

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private IdentifierIdxRepository identifierIdxRepository;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @MockBean
    private UserService userService;

    @Autowired
    private IdentifierService identifierService;

    @Test
    public void findAll_succeeds() {

        /* mock */
        when(identifierRepository.findAll())
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final List<Identifier> response = identifierService.findAll();
        assertEquals(1, response.size());
        assertEquals(IDENTIFIER_1, response.get(0));
    }

    @Test
    public void findAll2_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.findAll())
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final List<Identifier> response = identifierService.findAll(null, null);
        assertEquals(1, response.size());
        assertEquals(IDENTIFIER_1, response.get(0));
    }

    @Test
    public void findAll2_databaseId_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.findByDatabaseId(DATABASE_1_ID))
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_1_ID, null);
        assertEquals(1, response.size());
        assertEquals(IDENTIFIER_1, response.get(0));
    }

    @Test
    public void findAll2_queryId_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.findByQueryId(QUERY_1_ID))
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final List<Identifier> response = identifierService.findAll(null, QUERY_1_ID);
        assertEquals(1, response.size());
        assertEquals(IDENTIFIER_1, response.get(0));
    }

    @Test
    public void findAll2_databaseIdAndQueryId_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_1_ID, QUERY_1_ID);
        assertEquals(1, response.size());
        assertEquals(IDENTIFIER_1, response.get(0));
    }

    @Test
    public void find_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final Identifier response = identifierService.find(IDENTIFIER_1_ID);
        assertEquals(IDENTIFIER_1, response);
    }

    @Test
    public void findByDatabaseIdAndQueryId_succeeds() {

        /* mock */
        when(identifierRepository.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final List<Identifier> response = identifierService.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID);
        assertEquals(1, response.size());
        final Identifier identifier0 = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier0.getId());
    }

    @Test
    public void findByDatabaseIdAndQueryId_fails() {

        /* mock */
        when(identifierRepository.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(List.of());

        /* test */
        final List<Identifier> response = identifierService.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void find_fails() {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.find(IDENTIFIER_1_ID);
        });
    }

    @Test
    public void create_database_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException,
            IdentifierRequestException {
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.ok(QUERY_1_DTO));
        when(userService.findByUsername(USER_1_USERNAME))
                .thenReturn(USER_1);
        when(identifierRepository.save(any(Identifier.class)))
                .thenReturn(IDENTIFIER_1);
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_1_DTO);


        /* test */
        identifierService.create(IDENTIFIER_1_DTO_REQUEST, USER_1_PRINCIPAL, bearer);
    }

    @Test
    public void create_existsSubset_fails()
            throws DatabaseNotFoundException {
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(databaseService.find(DATABASE_2_ID))
                .thenReturn(DATABASE_2);
        when(identifierRepository.existsByDatabaseIdAndQueryIdAndType(DATABASE_2_ID, QUERY_2_ID, IdentifierType.SUBSET))
                .thenReturn(true);


        /* test */
        assertThrows(IdentifierAlreadyExistsException.class, () -> {
            identifierService.create(IDENTIFIER_2_DTO_REQUEST, USER_1_PRINCIPAL, bearer);
        });
    }

    @Test
    public void create_existsDatabase_fails() throws DatabaseNotFoundException {
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(databaseService.find(DATABASE_1_ID))
                .thenReturn(DATABASE_1);
        when(identifierRepository.existsByDatabaseIdAndType(DATABASE_1_ID, IdentifierType.DATABASE))
                .thenReturn(true);


        /* test */
        assertThrows(IdentifierAlreadyExistsException.class, () -> {
            identifierService.create(IDENTIFIER_1_DTO_REQUEST, USER_1_PRINCIPAL, bearer);
        });
    }

    @Test
    public void exportBibliography_apa_succeeds() throws IdentifierNotFoundException, IdentifierRequestException {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.APA);
        assertTrue(response.contains(IDENTIFIER_1_TITLE_1.getTitle()));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
        assertTrue(response.contains(IDENTIFIER_1_CREATOR_1.getLastname()));
    }

    @Test
    public void exportBibliography_apaMixedPersonAndOrg_succeeds() throws IdentifierNotFoundException,
            IdentifierRequestException {
        final Creator org = Creator.builder()
                .id(CREATOR_2_ID)
                .creatorName("Institute of Science and Technology Austria")
                .nameIdentifier("https://ror.org/03gnh5541")
                .nameIdentifierScheme(NameIdentifierSchemeType.ROR)
                .build();
        final Identifier identifier = IDENTIFIER_1.toBuilder()
                .creators(List.of(IDENTIFIER_1_CREATOR_1, org))
                .build();

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(identifier));

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.APA);
        final String title = IDENTIFIER_1_CREATOR_1.getFirstname().charAt(0) + "., " + IDENTIFIER_1_CREATOR_1.getLastname() + " & Institute of Science and Technology Austria";
        assertTrue(response.contains(title));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
    }

    @Test
    public void exportBibliography_bibtex_succeeds() throws IdentifierNotFoundException, IdentifierRequestException {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.BIBTEX);
        assertTrue(response.contains(IDENTIFIER_1_TITLE_1.getTitle()));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
        assertTrue(response.contains(IDENTIFIER_1_CREATOR_1.getLastname()));
    }

    @Test
    public void exportBibliography_bibtexMixedPersonAndOrg_succeeds() throws IdentifierNotFoundException,
            IdentifierRequestException {
        final Creator org = Creator.builder()
                .id(CREATOR_2_ID)
                .creatorName("Institute of Science and Technology Austria")
                .nameIdentifier("https://ror.org/03gnh5541")
                .nameIdentifierScheme(NameIdentifierSchemeType.ROR)
                .build();
        final Identifier identifier = IDENTIFIER_1.toBuilder()
                .creators(List.of(IDENTIFIER_1_CREATOR_1, org))
                .build();

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(identifier));

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.BIBTEX);
        final String title = IDENTIFIER_1_CREATOR_1.getLastname() + ", " + IDENTIFIER_1_CREATOR_1.getFirstname() + " and Institute of Science and Technology Austria";
        assertTrue(response.contains(title));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
    }

    @Test
    public void exportBibliography_ieee_succeeds() throws IdentifierNotFoundException, IdentifierRequestException {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.IEEE);
        assertTrue(response.contains(IDENTIFIER_1_TITLE_1.getTitle()));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
        assertTrue(response.contains(IDENTIFIER_1_CREATOR_1.getLastname()));
    }

    @Test
    public void exportBibliography_ieeeMixedPersonAndOrg_succeeds() throws IdentifierNotFoundException,
            IdentifierRequestException {
        final Creator org = Creator.builder()
                .id(CREATOR_2_ID)
                .creatorName("Institute of Science and Technology Austria")
                .nameIdentifier("https://ror.org/03gnh5541")
                .nameIdentifierScheme(NameIdentifierSchemeType.ROR)
                .build();
        final Identifier identifier = IDENTIFIER_1.toBuilder()
                .creators(List.of(IDENTIFIER_1_CREATOR_1, org))
                .build();

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(identifier));

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1_ID, BibliographyTypeDto.IEEE);
        final String title = IDENTIFIER_1_CREATOR_1.getFirstname().charAt(0) + ". " + IDENTIFIER_1_CREATOR_1.getLastname() + ", Institute of Science and Technology Austria";
        assertTrue(response.contains(title));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
    }

    @Test
    public void delete_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.existsById(IDENTIFIER_1_ID))
                .thenReturn(true);
        when(identifierIdxRepository.existsById(IDENTIFIER_1_ID))
                .thenReturn(true);
        doNothing()
                .when(identifierRepository)
                .delete(IDENTIFIER_1);
        doNothing()
                .when(identifierIdxRepository)
                .deleteById(IDENTIFIER_1_ID);

        /* test */
        identifierService.delete(IDENTIFIER_1_ID);
    }

    @Test
    public void delete_notFound_fails() {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.empty());
        doNothing()
                .when(identifierRepository)
                .delete(IDENTIFIER_1);

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.delete(IDENTIFIER_1_ID);
        });
    }

    @Test
    public void exportMetadata_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final InputStreamResource response = identifierService.exportMetadata(IDENTIFIER_1_ID);
        assertNotNull(response);
    }

    @Test
    public void exportMetadata_notFound_fails() {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.exportMetadata(IDENTIFIER_1_ID);
        });
    }

    @Test
    public void exportResource_database_fails() {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_4_ID))
                .thenReturn(Optional.of(IDENTIFIER_4));

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            identifierService.exportResource(IDENTIFIER_4_ID);
        });
    }

}
