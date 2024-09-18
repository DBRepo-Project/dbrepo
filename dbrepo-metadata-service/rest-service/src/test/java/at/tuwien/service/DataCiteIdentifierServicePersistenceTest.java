package at.tuwien.service;

import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.entities.identifier.Creator;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierStatusType;
import at.tuwien.entities.identifier.NameIdentifierSchemeType;
import at.tuwien.repository.ContainerRepository;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.repository.LicenseRepository;
import at.tuwien.repository.UserRepository;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.datacite.DataCiteBody;
import at.tuwien.api.datacite.doi.DataCiteDoi;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.gateway.SearchServiceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(properties = "spring.profiles.active:local,junit,doi")
public class DataCiteIdentifierServicePersistenceTest extends AbstractUnitTest {

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @MockBean
    @Qualifier("dataCiteRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private IdentifierService dataCiteIdentifierService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    private final ParameterizedTypeReference<DataCiteBody<DataCiteDoi>> dataCiteBodyParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4, USER_5));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2, CONTAINER_3, CONTAINER_4));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2, DATABASE_3, DATABASE_4));
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Identifier> response = dataCiteIdentifierService.findAll(null, null, null, null, null);
        assertEquals(7, response.size());
        for (Long id : List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID, IDENTIFIER_3_ID, IDENTIFIER_4_ID, IDENTIFIER_5_ID, IDENTIFIER_6_ID, IDENTIFIER_7_ID)) {
            assertTrue(response.stream().map(Identifier::getId).toList().contains(id));
        }
    }

    @Test
    public void findAll_databaseId_succeeds() {

        /* test */
        final List<Identifier> response = dataCiteIdentifierService.findAll(null, DATABASE_1_ID, null, null, null);
        assertEquals(4, response.size());
        assertTrue(response.stream().map(Identifier::getId).toList().contains(IDENTIFIER_1_ID));
        assertTrue(response.stream().map(Identifier::getId).toList().contains(IDENTIFIER_2_ID));
        assertTrue(response.stream().map(Identifier::getId).toList().contains(IDENTIFIER_3_ID));
        assertTrue(response.stream().map(Identifier::getId).toList().contains(IDENTIFIER_4_ID));
    }

    @Test
    public void findAll_queryId_succeeds() {

        /* test */
        final List<Identifier> response = dataCiteIdentifierService.findAll(null, null, QUERY_1_ID, null, null);
        assertEquals(1, response.size());
    }

    @Test
    public void findAll_empty_succeeds() {

        /* test */
        final List<Identifier> response = dataCiteIdentifierService.findAll(null, DATABASE_2_ID, QUERY_1_ID, null, null);
        assertEquals(0, response.size());
    }

    @Test
    public void find_succeeds() throws IdentifierNotFoundException {

        /* test */
        final Identifier response = dataCiteIdentifierService.find(IDENTIFIER_1_ID);
        assertEquals(IDENTIFIER_1, response);
    }

    @Test
    public void save_database_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, MalformedException, IdentifierNotFoundException, ViewNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException {
        final ResponseEntity<DataCiteBody<DataCiteDoi>> mock = ResponseEntity.status(HttpStatus.CREATED)
                .body(IDENTIFIER_1_DATA_CITE);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference)))
                .thenReturn(mock);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        dataCiteIdentifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
    }

    @Test
    public void save_invalidMetadata_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference));
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        assertThrows(MalformedException.class, () -> {
            dataCiteIdentifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
        });
    }

    @Test
    public void save_restClientException_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference));
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataCiteIdentifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
        });
    }

    @Test
    public void create_succeeds() throws SearchServiceException, MalformedException, DataServiceException,
            QueryNotFoundException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException,
            ExternalServiceException {
        final ResponseEntity<DataCiteBody<DataCiteDoi>> mock = ResponseEntity.status(HttpStatus.CREATED)
                .body(IDENTIFIER_1_DATA_CITE);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference)))
                .thenReturn(mock);

        /* test */
        final Identifier response = dataCiteIdentifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_DTO);
        assertNotNull(response.getDoi());
    }

    @Test
    public void create_hasDoi_succeeds() throws SearchServiceException, MalformedException, DataServiceException,
            QueryNotFoundException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException,
            ExternalServiceException {
        final ResponseEntity<DataCiteBody<DataCiteDoi>> mock = ResponseEntity.status(HttpStatus.CREATED)
                .body(IDENTIFIER_1_DATA_CITE);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference)))
                .thenReturn(mock);

        /* test */
        final Identifier response = dataCiteIdentifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_WITH_DOI_DTO);
        assertEquals(IDENTIFIER_1_DOI_NOT_NULL, response.getDoi());
    }

    @Test
    public void publish_succeeds() throws MalformedException, DataServiceConnectionException, SearchServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, IdentifierNotFoundException,
            ExternalServiceException {
        final ResponseEntity<DataCiteBody<DataCiteDoi>> mock = ResponseEntity.status(HttpStatus.CREATED)
                .body(IDENTIFIER_7_DATA_CITE);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference)))
                .thenReturn(mock);

        /* test */
        final Identifier response = dataCiteIdentifierService.publish(IDENTIFIER_7_ID);
        assertEquals(IDENTIFIER_7_ID, response.getId());
        assertEquals(IdentifierStatusType.PUBLISHED, response.getStatus());
    }

    @Test
    public void exportMetadata_succeeds() {

        /* test */
        final InputStreamResource response = dataCiteIdentifierService.exportMetadata(IDENTIFIER_1);
        assertNotNull(response);
    }

    @Test
    public void exportBibliography_apa_succeeds() throws MalformedException {

        /* test */
        final String response = dataCiteIdentifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.APA);
        assertTrue(response.contains(IDENTIFIER_1_TITLE_1.getTitle()));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
        assertTrue(response.contains(IDENTIFIER_1_CREATOR_1.getLastname()));
    }

    @Test
    public void exportBibliography_apaMixedPersonAndOrg_succeeds() throws MalformedException {
        final Creator org = Creator.builder()
                .id(CREATOR_2_ID)
                .creatorName("Institute of Science and Technology Austria")
                .nameIdentifier("https://ror.org/03gnh5541")
                .nameIdentifierScheme(NameIdentifierSchemeType.ROR)
                .build();
        final Identifier identifier = IDENTIFIER_1.toBuilder()
                .creators(List.of(IDENTIFIER_1_CREATOR_1, org))
                .build();

        /* test */
        final String response = dataCiteIdentifierService.exportBibliography(identifier, BibliographyTypeDto.APA);
        final String title = IDENTIFIER_1_CREATOR_1.getFirstname().charAt(0) + "., " + IDENTIFIER_1_CREATOR_1.getLastname() + " & Institute of Science and Technology Austria";
        assertTrue(response.contains(title), "expected title not found: " + title);
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR), "expected publication year not found: " + IDENTIFIER_1_PUBLICATION_YEAR);
    }

    @Test
    public void exportBibliography_bibtex_succeeds() throws MalformedException {

        /* test */
        final String response = dataCiteIdentifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.BIBTEX);
        assertTrue(response.contains(IDENTIFIER_1_TITLE_1.getTitle()));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
        assertTrue(response.contains(IDENTIFIER_1_CREATOR_1.getLastname()));
    }

    @Test
    public void exportBibliography_bibtexMixedPersonAndOrg_succeeds() throws MalformedException {
        final Creator org = Creator.builder()
                .id(CREATOR_2_ID)
                .creatorName("Institute of Science and Technology Austria")
                .nameIdentifier("https://ror.org/03gnh5541")
                .nameIdentifierScheme(NameIdentifierSchemeType.ROR)
                .build();
        final Identifier identifier = IDENTIFIER_1.toBuilder()
                .creators(List.of(IDENTIFIER_1_CREATOR_1, org))
                .build();

        /* test */
        final String response = dataCiteIdentifierService.exportBibliography(identifier, BibliographyTypeDto.BIBTEX);
        final String title = IDENTIFIER_5_CREATOR_1.getLastname() + ", " + IDENTIFIER_1_CREATOR_1.getFirstname() + " and Institute of Science and Technology Austria";
        assertTrue(response.contains(title), "expected title not found: " + title);
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR), "expected publication year not found: " + IDENTIFIER_1_PUBLICATION_YEAR);
    }

    @Test
    public void exportBibliography_ieee_succeeds() throws MalformedException {

        /* test */
        final String response = dataCiteIdentifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.IEEE);
        assertTrue(response.contains(IDENTIFIER_1_TITLE_1.getTitle()));
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR));
        assertTrue(response.contains(IDENTIFIER_1_CREATOR_1.getLastname()));
    }

    @Test
    public void exportBibliography_ieeeMixedPersonAndOrg_succeeds() throws MalformedException {
        final Creator org = Creator.builder()
                .id(CREATOR_2_ID)
                .creatorName("Institute of Science and Technology Austria")
                .nameIdentifier("https://ror.org/03gnh5541")
                .nameIdentifierScheme(NameIdentifierSchemeType.ROR)
                .build();
        final Identifier identifier = IDENTIFIER_1.toBuilder()
                .creators(List.of(IDENTIFIER_1_CREATOR_1, org))
                .build();

        /* test */
        final String response = dataCiteIdentifierService.exportBibliography(identifier, BibliographyTypeDto.IEEE);
        final String title = IDENTIFIER_1_CREATOR_1.getFirstname().charAt(0) + ". " + IDENTIFIER_1_CREATOR_1.getLastname() + ", Institute of Science and Technology Austria";
        assertTrue(response.contains(title), "expected title not found: " + title);
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR), "expected publication year not found: " + IDENTIFIER_1_PUBLICATION_YEAR);
    }

    @Test
    public void delete_succeeds() throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            IdentifierNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        dataCiteIdentifierService.delete(IDENTIFIER_1);
    }

}
