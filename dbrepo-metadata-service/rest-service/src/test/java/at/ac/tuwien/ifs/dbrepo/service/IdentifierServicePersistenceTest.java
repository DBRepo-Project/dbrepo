package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.identifier.BibliographyTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.License;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class IdentifierServicePersistenceTest extends BaseTest {

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @MockBean
    private ViewService viewService;

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
    @Transactional
    public void findAll_queryId_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(null, null, QUERY_1_ID, null, null);
        assertEquals(1, response.size());
        final Identifier identifier0 = response.get(0);
        assertIdentifierEquals(IDENTIFIER_2, identifier0);
    }

    @Test
    public void findAll_empty_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(null, DATABASE_2_ID, QUERY_1_ID, null, null);
        assertEquals(0, response.size());
    }

    @Test
    @Transactional
    public void find_succeeds() throws IdentifierNotFoundException {

        /* test */
        final Identifier response = identifierService.find(IDENTIFIER_1_ID);
        assertIdentifierEquals(IDENTIFIER_1, response);
    }

    @Test
    @Transactional
    public void findByDatabaseIdAndQueryId_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findByDatabaseIdAndQueryId(DATABASE_2_ID, QUERY_2_ID);
        assertEquals(1, response.size());
        assertIdentifierEquals(IDENTIFIER_5, response.get(0));
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


        /* test */
        final Identifier response = identifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
        assertIdentifierEquals(IDENTIFIER_1, response);
    }

    @Test
    public void save_idempotent_succeeds() throws DataServiceException, DataServiceConnectionException,
            MalformedException, DatabaseNotFoundException, IdentifierNotFoundException, ViewNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException {


        /* test */
        final Identifier response0 = identifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
        assertIdentifierEquals(IDENTIFIER_1, response0);
        final Identifier response1 = identifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
        assertIdentifierEquals(IDENTIFIER_1, response1);
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
        assertIdentifierEquals(IDENTIFIER_5, identifierService.save(DATABASE_2, USER_2, IDENTIFIER_5_SAVE_DTO));
    }

    @Test
    public void save_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, IdentifierNotFoundException, ViewNotFoundException, QueryNotFoundException,
            SearchServiceException, SearchServiceConnectionException, ExternalServiceException {

        /* test */
        assertIdentifierEquals(IDENTIFIER_1, identifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO));
    }

    @Test
    public void save_noRelatedTitleDescription_succeeds() throws DataServiceException, DataServiceConnectionException,
            MalformedException, DatabaseNotFoundException, IdentifierNotFoundException, ViewNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException {

        /* test */
        assertIdentifierEquals(IDENTIFIER_7, identifierService.save(DATABASE_4, USER_4, IDENTIFIER_7_SAVE_DTO));
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
        assertIdentifierEquals(IDENTIFIER_2, identifierService.save(DATABASE_1, USER_1, IDENTIFIER_2_SAVE_DTO));
    }

    @Test
    public void save_viewIdentifier_succeeds() throws SearchServiceException, MalformedException, DataServiceException,
            QueryNotFoundException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException,
            ExternalServiceException {

        /* mock */
        when(viewService.findById(DATABASE_1, VIEW_1_ID))
                .thenReturn(VIEW_1);

        /* test */
        assertIdentifierEquals(IDENTIFIER_3, identifierService.save(DATABASE_1, USER_1, IDENTIFIER_3_SAVE_DTO));
    }

    @Test
    public void create_succeeds() throws MalformedException, DataServiceConnectionException, SearchServiceException,
            DataServiceException, QueryNotFoundException, DatabaseNotFoundException, SearchServiceConnectionException,
            IdentifierNotFoundException, ViewNotFoundException, ExternalServiceException {

        /* test */
        assertIdentifierEquals(IDENTIFIER_1, identifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_DTO));
    }

    @Test
    public void create_hasDoi_succeeds() throws SearchServiceException, MalformedException, DataServiceException,
            QueryNotFoundException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, ViewNotFoundException, ExternalServiceException,
            IdentifierNotFoundException {

        /* test */
        assertIdentifierEquals(IDENTIFIER_1, identifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_WITH_DOI_DTO));
    }

    @Test
    public void publish_succeeds() throws MalformedException, DataServiceConnectionException, SearchServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, ExternalServiceException {

        /* test */
        assertIdentifierEquals(IDENTIFIER_7, identifierService.publish(IDENTIFIER_7));
    }

    @Transactional
    public void assertIdentifierEquals(Identifier expected, Identifier actual) {
        assertIdentifierEquals(expected, actual, false);
    }

    @Transactional
    public void assertIdentifierEquals(Identifier expected, Identifier actual, boolean withIds) {
        if (withIds) {
            assertEquals(expected.getId(), actual.getId());
        }
        assertEquals(expected.getQueryId(), actual.getQueryId());
        assertEquals(expected.getQueryId(), actual.getQueryId());
        assertEquals(expected.getTableId(), actual.getTableId());
        assertEquals(expected.getViewId(), actual.getViewId());
        assertEquals(expected.getCreators().size(), actual.getCreators().size());
        for (int i = 0; i < expected.getCreators().size(); i++) {
            final Creator expectedCreator = expected.getCreators().get(i);
            final Creator actualCreator = actual.getCreators().get(i);
            if (withIds) {
                assertEquals(expectedCreator.getId(), actualCreator.getId());
            }
            assertEquals(expectedCreator.getOrdinalPosition(), actualCreator.getOrdinalPosition());
            assertEquals(expectedCreator.getFirstname(), actualCreator.getFirstname());
            assertEquals(expectedCreator.getLastname(), actualCreator.getLastname());
            assertEquals(expectedCreator.getCreatorName(), actualCreator.getCreatorName());
            assertEquals(expectedCreator.getNameType(), actualCreator.getNameType());
            assertEquals(expectedCreator.getNameIdentifierScheme(), actualCreator.getNameIdentifierScheme());
            assertEquals(expectedCreator.getNameIdentifierSchemeUri(), actualCreator.getNameIdentifierSchemeUri());
            assertEquals(expectedCreator.getAffiliation(), actualCreator.getAffiliation());
            assertEquals(expectedCreator.getAffiliationIdentifier(), actualCreator.getAffiliationIdentifier());
            assertEquals(expectedCreator.getAffiliationIdentifierScheme(), actualCreator.getAffiliationIdentifierScheme());
            assertEquals(expectedCreator.getAffiliationIdentifierSchemeUri(), actualCreator.getAffiliationIdentifierSchemeUri());
            assertNotNull(actualCreator.getIdentifier());
        }
        assertEquals(expected.getPublisher(), actual.getPublisher());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getLanguage(), actual.getLanguage());
        assertEquals(expected.getTitles().size(), actual.getTitles().size());
        for (int i = 0; i < expected.getTitles().size(); i++) {
            final IdentifierTitle expectedTitle = expected.getTitles().get(i);
            final IdentifierTitle actualTitle = actual.getTitles().get(i);
            if (withIds) {
                assertEquals(expectedTitle.getId(), actualTitle.getId());
            }
            assertEquals(expectedTitle.getOrdinalPosition(), actualTitle.getOrdinalPosition());
            assertEquals(expectedTitle.getTitle(), actualTitle.getTitle());
            assertEquals(expectedTitle.getTitleType(), actualTitle.getTitleType());
            assertEquals(expectedTitle.getLanguage(), actualTitle.getLanguage());
            assertNotNull(expectedTitle.getIdentifier());
        }
        assertEquals(expected.getDescriptions().size(), actual.getDescriptions().size());
        for (int i = 0; i < expected.getDescriptions().size(); i++) {
            final IdentifierDescription expectedDescription = expected.getDescriptions().get(i);
            final IdentifierDescription actualDescription = actual.getDescriptions().get(i);
            if (withIds) {
                assertEquals(expectedDescription.getId(), actualDescription.getId());
            }
            assertEquals(expectedDescription.getOrdinalPosition(), actualDescription.getOrdinalPosition());
            assertEquals(expectedDescription.getDescription(), actualDescription.getDescription());
            assertEquals(expectedDescription.getDescriptionType(), actualDescription.getDescriptionType());
            assertEquals(expectedDescription.getLanguage(), actualDescription.getLanguage());
            assertNotNull(expectedDescription.getIdentifier());
        }
        assertEquals(expected.getFunders().size(), actual.getFunders().size());
        for (int i = 0; i < expected.getFunders().size(); i++) {
            final IdentifierFunder expectedFunder = expected.getFunders().get(i);
            final IdentifierFunder actualFunder = actual.getFunders().get(i);
            if (withIds) {
                assertEquals(expectedFunder.getId(), actualFunder.getId());
            }
            assertEquals(expectedFunder.getOrdinalPosition(), actualFunder.getOrdinalPosition());
            assertEquals(expectedFunder.getFunderName(), actualFunder.getFunderName());
            assertEquals(expectedFunder.getFunderIdentifierType(), actualFunder.getFunderIdentifierType());
            assertEquals(expectedFunder.getSchemeUri(), actualFunder.getSchemeUri());
            assertEquals(expectedFunder.getAwardNumber(), actualFunder.getAwardNumber());
            assertEquals(expectedFunder.getAwardTitle(), actualFunder.getAwardTitle());
            assertNotNull(expectedFunder.getIdentifier());
        }
        assertEquals(expected.getLicenses().size(), actual.getLicenses().size());
        for (int i = 0; i < expected.getLicenses().size(); i++) {
            final License expectedLicense = expected.getLicenses().get(i);
            final License actualLicense = actual.getLicenses().get(i);
            assertEquals(expectedLicense.getIdentifier(), actualLicense.getIdentifier());
            assertEquals(expectedLicense.getDescription(), actualLicense.getDescription());
            assertEquals(expectedLicense.getUri(), actualLicense.getUri());
        }
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getQuery(), actual.getQuery());
        assertEquals(expected.getQueryHash(), actual.getQueryHash());
        assertEquals(expected.getResultHash(), actual.getResultHash());
        assertEquals(expected.getResultNumber(), actual.getResultNumber());
        assertEquals(expected.getExecution(), actual.getExecution());
        assertEquals(expected.getPublicationDay(), actual.getPublicationDay());
        assertEquals(expected.getPublicationMonth(), actual.getPublicationMonth());
        assertEquals(expected.getPublicationYear(), actual.getPublicationYear());
        assertNotNull(actual.getDatabase());
        assertEquals(expected.getRelatedIdentifiers().size(), actual.getRelatedIdentifiers().size());
        for (int i = 0; i < expected.getRelatedIdentifiers().size(); i++) {
            final RelatedIdentifier expectedRelated = expected.getRelatedIdentifiers().get(i);
            final RelatedIdentifier actualRelated = actual.getRelatedIdentifiers().get(i);
            if (withIds) {
                assertEquals(expectedRelated.getId(), actualRelated.getId());
            }
            assertEquals(expectedRelated.getOrdinalPosition(), actualRelated.getOrdinalPosition());
            assertEquals(expectedRelated.getValue(), actualRelated.getValue());
            assertEquals(expectedRelated.getType(), actualRelated.getType());
            assertEquals(expectedRelated.getRelation(), actualRelated.getRelation());
            assertNotNull(actualRelated.getIdentifier());
        }
        assertEquals(expected.getDoi(), actual.getDoi());
        assertEquals(expected.getOwnedBy(), actual.getOwnedBy());
        assertNotNull(actual.getOwner());
    }
}
