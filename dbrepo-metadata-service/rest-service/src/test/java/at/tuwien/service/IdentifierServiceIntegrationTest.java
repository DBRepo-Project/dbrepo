package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierDescription;
import at.tuwien.entities.identifier.IdentifierTitle;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@MockAmqp
public class IdentifierServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private StoreService storeService;

    @MockBean
    @Qualifier("brokerRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private IdentifierService identifierService;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private IdentifierIdxRepository identifierIdxRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TableRepository tableRepository;

    @Container
    private static final OpensearchContainer opensearchContainer = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2.10.0"));

    @DynamicPropertySource
    static void openSearchProperties(DynamicPropertyRegistry registry) {
        final int idx = opensearchContainer.getHttpHostAddress().lastIndexOf(':');
        registry.add("spring.opensearch.host", () -> "127.0.0.1");
        registry.add("spring.opensearch.port", () -> opensearchContainer.getHttpHostAddress().substring(idx + 1));
        registry.add("spring.opensearch.username", opensearchContainer::getUsername);
        registry.add("spring.opensearch.password", opensearchContainer::getPassword);
    }

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1_SIMPLE);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4));
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);
        databaseRepository.save(DATABASE_2_SIMPLE);
    }

    @Test
    public void findAll_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<Identifier> response = identifierService.findAll();
        assertEquals(1, response.size());
    }

    @Test
    @Transactional
    public void find_succeeds() throws IdentifierNotFoundException {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final Identifier response = identifierService.find(IDENTIFIER_1_ID);
        assertEquals(IDENTIFIER_1_ID, response.getId());
        final List<IdentifierTitle> titles = response.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitle title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE, title0.getTitleType());
        final IdentifierTitle title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE, title1.getTitleType());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.find(IDENTIFIER_1_ID);
        });
    }

    @Test
    public void findAll_forDatabase_succeeds() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findAll_forDatabaseEmpty_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void create_subsetRelatedIdentifiers_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException,
            IdentifierRequestException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException {

        /* mock */
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.ok(QUERY_2_DTO));
        when(storeService.findOne(DATABASE_2_ID, IDENTIFIER_2_QUERY_ID, USER_2_PRINCIPAL))
                .thenReturn(QUERY_2);
        identifierRepository.save(IDENTIFIER_1_SIMPLE);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_2_DTO_REQUEST, USER_2_PRINCIPAL);
        assertEquals(IDENTIFIER_2_ID, response.getId());
        assertNotNull(response.getTitles());
        assertEquals(1, response.getTitles().size());
        final IdentifierTitle title0 = response.getTitles().get(0);
        assertEquals(IDENTIFIER_2_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_2_TITLE_1_LANG, title0.getLanguage());
        assertEquals(IDENTIFIER_2_TITLE_1_TYPE, title0.getTitleType());
        assertNotNull(response.getDescriptions());
        assertEquals(1, response.getDescriptions().size());
        final IdentifierDescription description0 = response.getDescriptions().get(0);
        assertEquals(IDENTIFIER_2_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_2_DESCRIPTION_1_LANG, description0.getLanguage());
        assertEquals(IDENTIFIER_2_DESCRIPTION_1_TYPE, description0.getDescriptionType());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_2_PUBLISHER, response.getPublisher());
        assertEquals(IDENTIFIER_2_DATABASE_ID, response.getDatabase().getId());
        assertNull(response.getLanguage());
        assertEquals(IDENTIFIER_2_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_2_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_2_PUBLICATION_DAY, response.getPublicationDay());
        final List<RelatedIdentifier> relatedIdentifiers = response.getRelatedIdentifiers();
        assertEquals(1, relatedIdentifiers.size());
        final RelatedIdentifier relatedIdentifier1 = relatedIdentifiers.get(0);
        assertEquals(RELATED_IDENTIFIER_2_ID, relatedIdentifier1.getId());
        assertEquals(RELATED_IDENTIFIER_2_TYPE, relatedIdentifier1.getType());
        assertEquals(RELATED_IDENTIFIER_2_RELATION_TYPE, relatedIdentifier1.getRelation());
        assertEquals(RELATED_IDENTIFIER_2_VALUE, relatedIdentifier1.getValue());
        /* open search database */
        assertTrue(identifierIdxRepository.existsById(IDENTIFIER_2_ID));
    }

    @Test
    public void create_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException {

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_1_DTO_REQUEST, USER_1_PRINCIPAL);
        assertEquals(IDENTIFIER_1_ID, response.getId());
        assertNotNull(response.getTitles());
        final List<IdentifierTitle> titles = response.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitle title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_ID, title0.getId());
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE, title0.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_1_LANG, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE, title0.getTitleType());
        final IdentifierTitle title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_ID, title1.getId());
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE, title1.getTitleType());
        assertNotNull(response.getDescriptions());
        assertEquals(1, response.getDescriptions().size());
        final List<IdentifierDescription> descriptions = response.getDescriptions();
        final IdentifierDescription description0 = descriptions.get(0);
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_DESCRIPTION, description0.getDescription());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_LANG, description0.getLanguage());
        assertEquals(IDENTIFIER_1_DESCRIPTION_1_TYPE, description0.getDescriptionType());
        assertNotNull(response.getCreators());
        assertEquals(1, response.getCreators().size());
        assertNotNull(response.getFunders());
        assertEquals(1, response.getFunders().size());
        /* open search database */
        assertTrue(identifierIdxRepository.existsById(IDENTIFIER_1_ID));
    }

    @Test
    public void create_noRelatedTitleDescription_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException {

        /* mock */
        containerRepository.save(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_4_SIMPLE);
        databaseRepository.save(DATABASE_3_SIMPLE);
        databaseRepository.save(DATABASE_4_SIMPLE);
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_2_SIMPLE);
        identifierRepository.save(IDENTIFIER_3_SIMPLE);
        identifierIdxRepository.save(IDENTIFIER_1_DTO);
        identifierIdxRepository.save(IDENTIFIER_2_DTO);
        identifierIdxRepository.save(IDENTIFIER_3_DTO);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_4_DTO_REQUEST, USER_1_PRINCIPAL);
        assertEquals(IDENTIFIER_4_ID, response.getId());
        assertNotNull(response.getTitles());
        assertEquals(0, response.getTitles().size());
        assertNotNull(response.getDescriptions());
        assertEquals(0, response.getDescriptions().size());
        assertNotNull(response.getCreators());
        assertEquals(1, response.getCreators().size());
        assertNotNull(response.getFunders());
        assertEquals(0, response.getFunders().size());
        /* open search database */
        assertTrue(identifierIdxRepository.existsById(IDENTIFIER_4_ID));
    }

    @Test
    public void create_subsetHasDatabaseIdentifier_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException {

        /* mock */
        containerRepository.save(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_4_SIMPLE);
        databaseRepository.save(DATABASE_3_SIMPLE);
        databaseRepository.save(DATABASE_4_SIMPLE);
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_2_SIMPLE);
        identifierRepository.save(IDENTIFIER_3_SIMPLE);
        identifierRepository.save(IDENTIFIER_4_SIMPLE);
        identifierIdxRepository.save(IDENTIFIER_1_DTO);
        identifierIdxRepository.save(IDENTIFIER_2_DTO);
        identifierIdxRepository.save(IDENTIFIER_3_DTO);
        identifierIdxRepository.save(IDENTIFIER_4_DTO);
        when(storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL))
                .thenReturn(QUERY_1);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_5_DTO_REQUEST, USER_1_PRINCIPAL);
        assertEquals(IDENTIFIER_5_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_5_DATABASE_ID, response.getDatabase().getId());
        assertEquals(IDENTIFIER_5_QUERY, response.getQuery());
        assertEquals(IDENTIFIER_5_QUERY_HASH, response.getQueryHash());
        assertEquals(IDENTIFIER_5_RESULT_HASH, response.getResultHash());
        assertEquals(0, response.getTitles().size());
        assertEquals(0, response.getDescriptions().size());
        /* open search database */
        assertTrue(identifierIdxRepository.existsById(IDENTIFIER_5_ID));
    }

    @Test
    public void create_viewIdentifier_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException {

        /* mock */
        containerRepository.save(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_4_SIMPLE);
        databaseRepository.save(DATABASE_3_SIMPLE);
        databaseRepository.save(DATABASE_4_SIMPLE);
        tableRepository.saveAll(List.of(TABLE_1_SIMPLE, TABLE_2_SIMPLE, TABLE_3_SIMPLE));
        viewRepository.save(VIEW_1);
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_2_SIMPLE);
        identifierRepository.save(IDENTIFIER_3_SIMPLE);
        identifierRepository.save(IDENTIFIER_4_SIMPLE);
        identifierRepository.save(IDENTIFIER_5_SIMPLE);
        identifierIdxRepository.save(IDENTIFIER_1_DTO);
        identifierIdxRepository.save(IDENTIFIER_2_DTO);
        identifierIdxRepository.save(IDENTIFIER_3_DTO);
        identifierIdxRepository.save(IDENTIFIER_4_DTO);
        identifierIdxRepository.save(IDENTIFIER_5_DTO);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_6_DTO_REQUEST, USER_1_PRINCIPAL);
        assertEquals(IDENTIFIER_6_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_6_DATABASE_ID, response.getDatabase().getId());
        assertEquals(IDENTIFIER_6_QUERY, response.getQuery());
        assertEquals(IDENTIFIER_6_QUERY_HASH, response.getQueryHash());
        assertEquals(IDENTIFIER_6_RESULT_HASH, response.getResultHash());
        assertEquals(0, response.getTitles().size());
        assertEquals(0, response.getDescriptions().size());
        assertEquals(1, response.getLicenses().size());
        /* open search database */
        assertTrue(identifierIdxRepository.existsById(IDENTIFIER_6_ID));
    }

    @Test
    public void update_database_succeeds() throws UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierRequestException,
            IdentifierNotFoundException, QueryStoreException, DatabaseConnectionException, ImageNotSupportedException {

        /* mock */
        identifierRepository.save(IDENTIFIER_1_SIMPLE);

        /* test */
        final Identifier response = identifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST, USER_1_PRINCIPAL);
        assertEquals(IDENTIFIER_1_ID, response.getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, response.getDatabaseId());
        final List<IdentifierTitle> titles = response.getTitles();
        assertEquals(2, titles.size());
        final IdentifierTitle title0 = titles.get(0);
        assertEquals(IDENTIFIER_1_TITLE_1_ID, title0.getId());
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE_MODIFY, title0.getTitle()); // <<<<<<
        assertEquals(IDENTIFIER_1_TITLE_1_LANG, title0.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_1_TYPE, title0.getTitleType());
        final IdentifierTitle title1 = titles.get(1);
        assertEquals(IDENTIFIER_1_TITLE_2_ID, title1.getId());
        assertEquals(IDENTIFIER_1_TITLE_2_TITLE_MODIFY, title1.getTitle());
        assertEquals(IDENTIFIER_1_TITLE_2_LANG, title1.getLanguage());
        assertEquals(IDENTIFIER_1_TITLE_2_TYPE, title1.getTitleType());
        assertEquals(IDENTIFIER_1_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_1_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_1_PUBLICATION_DAY, response.getPublicationDay());
        /* open search database */
        assertTrue(identifierIdxRepository.existsById(IDENTIFIER_1_ID));
    }

    @Test
    @Transactional
    public void update_subset_succeeds() throws UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierRequestException,
            IdentifierNotFoundException, QueryStoreException, DatabaseConnectionException, ImageNotSupportedException {

        /* mock */
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_2_SIMPLE);
        when(storeService.findOne(eq(IDENTIFIER_2_DATABASE_ID), eq(IDENTIFIER_2_QUERY_ID), any()))
                .thenReturn(QUERY_2);

        /* test */
        final Identifier response = identifierService.update(IDENTIFIER_2_ID, IDENTIFIER_2_DTO_UPDATE_REQUEST, USER_2_PRINCIPAL);
        assertEquals(IDENTIFIER_2_ID, response.getId());
        assertEquals(IDENTIFIER_2_DATABASE_ID, response.getDatabase().getId());
        assertEquals(1, response.getTitles().size());
        assertEquals(1, identifierRepository.findAll().stream().map(Identifier::getTitles).flatMap(List::stream).toList().size());
        assertEquals(IDENTIFIER_2_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_2_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_2_PUBLICATION_DAY, response.getPublicationDay());
        /* open search database */
        assertTrue(identifierIdxRepository.existsById(IDENTIFIER_2_ID));
    }

    @Test
    public void delete_succeeds() throws IdentifierNotFoundException {

        /* mock */
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
        identifierIdxRepository.save(IDENTIFIER_1_DTO);

        /* test */
        identifierService.delete(IDENTIFIER_1_ID);
        /* open search database */
        assertFalse(identifierIdxRepository.existsById(IDENTIFIER_1_ID));
    }

    @Test
    public void delete_notFound_fails() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1_SIMPLE);

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.delete(IDENTIFIER_2_ID);
        });
    }

}
