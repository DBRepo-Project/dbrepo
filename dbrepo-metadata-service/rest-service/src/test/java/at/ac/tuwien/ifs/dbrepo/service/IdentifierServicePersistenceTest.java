package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.BibliographyTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.License;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.repository.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.repository.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.repository.LicenseRepository;
import at.ac.tuwien.ifs.dbrepo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class IdentifierServicePersistenceTest extends BaseTest {

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private IdentifierService identifierService;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4, USER_5));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2, CONTAINER_3, CONTAINER_4));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2, DATABASE_3, DATABASE_4));
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(null, null, null, null, null);
        assertEquals(7, response.size());
        for (UUID id : List.of(IDENTIFIER_1_ID, IDENTIFIER_2_ID, IDENTIFIER_3_ID, IDENTIFIER_4_ID, IDENTIFIER_5_ID, IDENTIFIER_6_ID, IDENTIFIER_7.getId())) {
            assertTrue(response.stream().map(Identifier::getId).toList().contains(id));
        }
    }

    @Test
    public void findAll_databaseId_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(null, DATABASE_1_ID, null, null, null);
        assertEquals(4, response.size());
        assertTrue(response.stream().map(Identifier::getId).toList().contains(IDENTIFIER_1_ID));
        assertTrue(response.stream().map(Identifier::getId).toList().contains(IDENTIFIER_2_ID));
        assertTrue(response.stream().map(Identifier::getId).toList().contains(IDENTIFIER_3_ID));
        assertTrue(response.stream().map(Identifier::getId).toList().contains(IDENTIFIER_4_ID));
    }

    @Test
    public void findAll_queryId_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(null, null, QUERY_1_ID, null, null);
        assertEquals(2, response.size());
    }

    @Test
    public void findAll_empty_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(null, DATABASE_2_ID, QUERY_1_ID, null, null);
        assertEquals(0, response.size());
    }

    @Test
    @Transactional(readOnly = true)
    public void find_succeeds() throws IdentifierNotFoundException {

        /* test */
        final Identifier response = identifierService.find(IDENTIFIER_1_ID);
        assertEquals(IDENTIFIER_1, response);
    }

    @Test
    public void findByDatabaseIdAndQueryId_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findByDatabaseIdAndQueryId(DATABASE_1_ID, QUERY_1_ID);
        assertEquals(2, response.size());
        final Identifier identifier0 = response.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier0.getId());
        final Identifier identifier1 = response.get(1);
        assertEquals(IDENTIFIER_2_ID, identifier1.getId());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.find(UUID.randomUUID());
        });
    }

    @Test
    public void save_database_succeeds() throws DataServiceException, DataServiceConnectionException,
            MalformedException, DatabaseNotFoundException, IdentifierNotFoundException, ViewNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException {

        /* mock */
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.ok(QUERY_1_DTO));


        /* test */
        identifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
    }

    @Test
    public void save_existsSubset_succeeds() throws DataServiceException, DataServiceConnectionException,
            MalformedException, DatabaseNotFoundException, IdentifierNotFoundException, ViewNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException {

        /* mock */
        when(dataServiceGateway.findQuery(DATABASE_2_ID, QUERY_2_ID))
                .thenReturn(QUERY_2_DTO);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_2_BRIEF_DTO);

        /* test */
        identifierService.save(DATABASE_2, USER_2, IDENTIFIER_5_SAVE_DTO);
    }

    @Test
    public void save_existsDatabase_succeeds() throws MalformedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, IdentifierNotFoundException,
            ViewNotFoundException, QueryNotFoundException, SearchServiceException, SearchServiceConnectionException,
            ExternalServiceException {

        /* test */
        identifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
    }

    @Test
    public void exportBibliography_apa_succeeds() throws MalformedException {

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.APA);
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
        final String response = identifierService.exportBibliography(identifier, BibliographyTypeDto.APA);
        final String title = IDENTIFIER_1_CREATOR_1.getFirstname().charAt(0) + "., " + IDENTIFIER_1_CREATOR_1.getLastname() + " & Institute of Science and Technology Austria";
        assertTrue(response.contains(title), "expected title not found: " + title);
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR), "expected publication year not found: " + IDENTIFIER_1_PUBLICATION_YEAR);
    }

    @Test
    public void exportBibliography_bibtex_succeeds() throws MalformedException {

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.BIBTEX);
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
        final String response = identifierService.exportBibliography(identifier, BibliographyTypeDto.BIBTEX);
        final String title = IDENTIFIER_5_CREATOR_1.getLastname() + ", " + IDENTIFIER_1_CREATOR_1.getFirstname() + " and Institute of Science and Technology Austria";
        assertTrue(response.contains(title), "expected title not found: " + title);
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR), "expected publication year not found: " + IDENTIFIER_1_PUBLICATION_YEAR);
    }

    @Test
    public void exportBibliography_ieee_succeeds() throws MalformedException {

        /* test */
        final String response = identifierService.exportBibliography(IDENTIFIER_1, BibliographyTypeDto.IEEE);
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
        final String response = identifierService.exportBibliography(identifier, BibliographyTypeDto.IEEE);
        final String title = IDENTIFIER_1_CREATOR_1.getFirstname().charAt(0) + ". " + IDENTIFIER_1_CREATOR_1.getLastname() + ", Institute of Science and Technology Austria";
        assertTrue(response.contains(title), "expected title not found: " + title);
        assertTrue(response.contains("" + IDENTIFIER_1_PUBLICATION_YEAR), "expected publication year not found: " + IDENTIFIER_1_PUBLICATION_YEAR);
    }

    @Test
    public void delete_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, IdentifierNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        identifierService.delete(IDENTIFIER_1);
    }

    @Test
    public void exportMetadata_succeeds() {

        /* test */
        final InputStreamResource response = identifierService.exportMetadata(IDENTIFIER_1);
        assertNotNull(response);
    }

    @Test
    @Transactional
    public void save_subsetRelatedIdentifiers_succeeds() throws DataServiceException, DataServiceConnectionException,
            MalformedException, DatabaseNotFoundException, IdentifierNotFoundException, ViewNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException {

        /* mock */
        when(dataServiceGateway.findQuery(DATABASE_2_ID, QUERY_2_ID))
                .thenReturn(QUERY_2_DTO);

        /* test */
        final Identifier response = identifierService.save(DATABASE_2, USER_2, IDENTIFIER_5_SAVE_DTO);
        assertNotNull(response.getTitles());
        assertEquals(1, response.getTitles().size());
        final IdentifierTitle title0 = response.getTitles().get(0);
        assertEquals(IDENTIFIER_5_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_5_TITLE_1_LANG, title0.getLanguage());
        assertEquals(IDENTIFIER_5_TITLE_1_TYPE, title0.getTitleType());
        assertNotNull(response.getDescriptions());
        assertEquals(1, response.getDescriptions().size());
        final IdentifierDescription description0 = response.getDescriptions().get(0);
        assertEquals(IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_5_DESCRIPTION_1_LANG, description0.getLanguage());
        assertEquals(IDENTIFIER_5_DESCRIPTION_1_TYPE, description0.getDescriptionType());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_5_PUBLISHER, response.getPublisher());
        assertEquals(DATABASE_2_ID, response.getDatabase().getId());
        assertNull(response.getLanguage());
        assertEquals(IDENTIFIER_5_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_5_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_5_PUBLICATION_DAY, response.getPublicationDay());
        assertNotNull(response.getRelatedIdentifiers());
        final List<RelatedIdentifier> relatedIdentifiers = response.getRelatedIdentifiers();
        assertEquals(1, relatedIdentifiers.size());
        final RelatedIdentifier relatedIdentifier1 = relatedIdentifiers.get(0);
        assertEquals(RELATED_IDENTIFIER_5_TYPE, relatedIdentifier1.getType());
        assertEquals(RELATED_IDENTIFIER_5_RELATION_TYPE, relatedIdentifier1.getRelation());
        assertEquals(RELATED_IDENTIFIER_5_VALUE, relatedIdentifier1.getValue());
    }

    @Test
    public void save_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, IdentifierNotFoundException, ViewNotFoundException, QueryNotFoundException,
            SearchServiceException, SearchServiceConnectionException, ExternalServiceException {

        /* test */
        final Identifier response = identifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
        assertNotNull(response.getTitles());
        final List<IdentifierTitle> titles = response.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitle title0 = titles.get(0);
        assertEquals(IDENTIFIER_1.getTitles().get(0).getTitle(), title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE, title0.getTitleType());
        final IdentifierTitle title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE, title1.getTitleType());
        assertNotNull(response.getDescriptions());
        assertEquals(1, response.getDescriptions().size());
        final List<IdentifierDescription> descriptions = response.getDescriptions();
        final IdentifierDescription description0 = descriptions.get(0);
        assertNotNull(description0.getId());
        assertEquals(IDENTIFIER_1.getDescriptions().get(0).getDescription(), description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG, description0.getLanguage());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_TYPE, description0.getDescriptionType());
        assertNotNull(response.getCreators());
        assertEquals(1, response.getCreators().size());
        final Creator creator0 = response.getCreators().get(0);
        assertNotNull(creator0.getId());
        assertEquals(IDENTIFIER_1_CREATOR_1_FIRSTNAME, creator0.getFirstname());
        assertEquals(IDENTIFIER_1_CREATOR_1_LASTNAME, creator0.getLastname());
        assertEquals(IDENTIFIER_1_CREATOR_1_NAME, creator0.getCreatorName());
        assertEquals(IDENTIFIER_1_CREATOR_1_ORCID, creator0.getNameIdentifier());
        assertEquals(IDENTIFIER_1_CREATOR_1_IDENTIFIER_SCHEME_TYPE, creator0.getNameIdentifierScheme());
        assertEquals(IDENTIFIER_1_CREATOR_1_AFFILIATION, creator0.getAffiliation());
        assertEquals(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER, creator0.getAffiliationIdentifier());
        assertEquals(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME, creator0.getAffiliationIdentifierScheme());
        assertEquals(IDENTIFIER_1_CREATOR_1_AFFILIATION_IDENTIFIER_SCHEME_URI, creator0.getAffiliationIdentifierSchemeUri());
        assertNotNull(response.getFunders());
        assertEquals(1, response.getFunders().size());
        assertNotNull(response.getRelatedIdentifiers());
        assertEquals(0, response.getRelatedIdentifiers().size());
    }

    @Test
    public void save_repeatedRemoveChildren_succeeds() throws MalformedException, DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, IdentifierNotFoundException,
            ViewNotFoundException, QueryNotFoundException, SearchServiceException, SearchServiceConnectionException,
            ExternalServiceException {

        /* test */
        final Identifier response = identifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_MODIFY_DTO);
        assertNotNull(response.getTitles());
        final List<IdentifierTitle> titles = response.getTitles();
        assertEquals(1, titles.size());
        final IdentifierTitle title0 = titles.get(0);
        assertEquals(IDENTIFIER_1.getTitles().get(0).getTitle(), title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE, title0.getTitleType());
        assertNotNull(response.getDescriptions());
        assertEquals(0, response.getDescriptions().size());
        assertNotNull(response.getCreators());
        assertEquals(0, response.getCreators().size());
        assertNotNull(response.getFunders());
        assertEquals(0, response.getFunders().size());
        assertNotNull(response.getLicenses());
        assertEquals(0, response.getLicenses().size());
        assertNotNull(response.getRelatedIdentifiers());
        assertEquals(0, response.getRelatedIdentifiers().size());
        final List<License> licenses = licenseRepository.findAll();
        assertEquals(1, licenses.size());

    }

    @Test
    public void save_noRelatedTitleDescription_succeeds() throws DataServiceException, DataServiceConnectionException,
            MalformedException, DatabaseNotFoundException, IdentifierNotFoundException, ViewNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException {

        /* test */
        final Identifier response = identifierService.save(DATABASE_4, USER_4, IDENTIFIER_7_SAVE_DTO);
        assertNotNull(response.getTitles());
        assertEquals(0, response.getTitles().size());
        assertNotNull(response.getDescriptions());
        assertEquals(0, response.getDescriptions().size());
        assertNotNull(response.getCreators());
        assertEquals(1, response.getCreators().size());
        assertNotNull(response.getFunders());
        assertEquals(0, response.getFunders().size());
    }

    @Test
    public void save_subsetHasDatabaseIdentifier_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, QueryNotFoundException, SearchServiceException, ViewNotFoundException,
            SearchServiceConnectionException, MalformedException, IdentifierNotFoundException,
            ExternalServiceException {

        /* mock */
        when(dataServiceGateway.findQuery(DATABASE_1_ID, QUERY_1_ID))
                .thenReturn(QUERY_1_DTO);

        /* test */
        final Identifier response = identifierService.save(DATABASE_1, USER_1, IDENTIFIER_2_SAVE_DTO);
        assertEquals(DATABASE_1_ID, response.getDatabase().getId());
        assertEquals(IDENTIFIER_2_QUERY, response.getQuery());
        assertEquals(IDENTIFIER_2_QUERY_HASH, response.getQueryHash());
        assertEquals(IDENTIFIER_2_RESULT_HASH, response.getResultHash());
        assertEquals(0, response.getTitles().size());
        assertEquals(0, response.getDescriptions().size());
    }

    @Test
    public void save_viewIdentifier_succeeds() throws SearchServiceException, MalformedException, DataServiceException,
            QueryNotFoundException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException,
            ExternalServiceException {

        /* test */
        final Identifier response = identifierService.save(DATABASE_1, USER_1, IDENTIFIER_3_SAVE_DTO);
        assertEquals(DATABASE_1_ID, response.getDatabase().getId());
        assertEquals(IDENTIFIER_3_QUERY, response.getQuery());
        assertEquals(IDENTIFIER_3_QUERY_HASH, response.getQueryHash());
        assertEquals(IDENTIFIER_3_RESULT_HASH, response.getResultHash());
        assertEquals(0, response.getTitles().size());
        assertEquals(0, response.getDescriptions().size());
        assertEquals(1, response.getLicenses().size());
    }

    @Test
    public void create_succeeds() throws MalformedException, DataServiceConnectionException, SearchServiceException,
            DataServiceException, QueryNotFoundException, DatabaseNotFoundException, SearchServiceConnectionException,
            IdentifierNotFoundException, ViewNotFoundException, ExternalServiceException {

        /* test */
        final Identifier response = identifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_DTO);
        assertNotNull(response.getId());
    }

    @Test
    public void create_hasDoi_succeeds() throws SearchServiceException, MalformedException, DataServiceException,
            QueryNotFoundException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, ViewNotFoundException, ExternalServiceException {

        /* test */
        final Identifier response = identifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_WITH_DOI_DTO);
        assertNotNull(response.getId());
        assertEquals(IDENTIFIER_1_DOI, response.getDoi());
    }

    @Test
    public void publish_succeeds() throws MalformedException, DataServiceConnectionException, SearchServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, ExternalServiceException {

        /* test */
        final Identifier response = identifierService.publish(IDENTIFIER_7);
        assertEquals(IDENTIFIER_7.getId(), response.getId());
        assertEquals(IdentifierStatusType.PUBLISHED, response.getStatus());
    }
}
