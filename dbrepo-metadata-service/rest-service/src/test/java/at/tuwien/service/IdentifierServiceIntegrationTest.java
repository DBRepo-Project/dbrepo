package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.*;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierDescription;
import at.tuwien.entities.identifier.IdentifierTitle;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.exception.*;
import at.tuwien.listener.MirrorListener;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
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

import javax.swing.text.html.Option;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Log4j2
@Testcontainers
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@MockAmqp
@MockListeners
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
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

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
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4));
        licenseRepository.save(LICENSE_1);
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2));
        DATABASE_1.setAccesses(List.of());
        DATABASE_2.setAccesses(List.of());
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2));
        /* search database */
        databaseIdxRepository.deleteAll();
        databaseIdxRepository.saveAll(List.of(DATABASE_1_DTO, DATABASE_2_DTO));
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll();
        assertEquals(5, response.size());
    }

    @Test
    @Transactional
    public void find_succeeds() throws IdentifierNotFoundException {

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
        /* open search database */
        final Optional<DatabaseDto> responseDto = databaseIdxRepository.findById(DATABASE_1_ID);
        assertTrue(responseDto.isPresent());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.find(9999L);
        });
    }

    @Test
    public void findAll_forDatabase_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_1_ID);
        assertEquals(4, response.size());
        /* open search database */
        final Optional<DatabaseDto> responseDto = databaseIdxRepository.findById(DATABASE_1_ID);
        assertTrue(responseDto.isPresent());
        final DatabaseDto databaseDto = responseDto.get();
        assertEquals(4, databaseDto.getIdentifiers().size());
    }

    @Test
    @Transactional
    public void create_subsetRelatedIdentifiers_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException,
            IdentifierRequestException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException, IdentifierNotFoundException {

        /* mock */
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.ok(QUERY_2_DTO));
        when(storeService.findOne(DATABASE_2_ID, IDENTIFIER_5_QUERY_ID, USER_2_PRINCIPAL))
                .thenReturn(QUERY_2);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_5_DTO_REQUEST, USER_2_PRINCIPAL);
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
        assertEquals(IDENTIFIER_5_DATABASE_ID, response.getDatabaseId());
        assertNull(response.getLanguage());
        assertEquals(IDENTIFIER_5_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_5_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_5_PUBLICATION_DAY, response.getPublicationDay());
        assertNotNull(response.getRelatedIdentifiers());
        final List<RelatedIdentifier> relatedIdentifiers = response.getRelatedIdentifiers();
        assertEquals(1, relatedIdentifiers.size());
        final RelatedIdentifier relatedIdentifier1 = relatedIdentifiers.get(0);
        assertEquals(RELATED_IDENTIFIER_5_ID, relatedIdentifier1.getId());
        assertEquals(RELATED_IDENTIFIER_5_TYPE, relatedIdentifier1.getType());
        assertEquals(RELATED_IDENTIFIER_5_RELATION_TYPE, relatedIdentifier1.getRelation());
        assertEquals(RELATED_IDENTIFIER_5_VALUE, relatedIdentifier1.getValue());
        /* open search database */
        final Optional<DatabaseDto> optional = databaseIdxRepository.findById(IDENTIFIER_5_DATABASE_ID);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get().getIdentifiers());
        assertEquals(2, optional.get().getIdentifiers().size());
        final IdentifierDto dto1 = optional.get().getIdentifiers().get(1);
        assertNotNull(dto1.getTitles());
        assertEquals(1, dto1.getTitles().size());
        final IdentifierTitleDto titleDto0 = dto1.getTitles().get(0);
        assertEquals(IDENTIFIER_5_TITLE_1_TITLE, titleDto0.getTitle());
        assertEquals(IDENTIFIER_5_TITLE_1_LANG_DTO, titleDto0.getLanguage());
        assertEquals(IDENTIFIER_5_TITLE_1_TYPE_DTO, titleDto0.getTitleType());
        assertNotNull(dto1.getDescriptions());
        assertEquals(1, dto1.getDescriptions().size());
        final IdentifierDescriptionDto descriptionDto0 = dto1.getDescriptions().get(0);
        assertEquals(IDENTIFIER_5_DESCRIPTION_1_DESCRIPTION, descriptionDto0.getDescription());
        assertEquals(IDENTIFIER_5_DESCRIPTION_1_LANG_DTO, descriptionDto0.getLanguage());
        assertEquals(IDENTIFIER_5_DESCRIPTION_1_TYPE_DTO, descriptionDto0.getDescriptionType());
        assertNull(dto1.getDoi());
        assertEquals(IDENTIFIER_5_PUBLISHER, dto1.getPublisher());
        assertNull(dto1.getLanguage());
        assertEquals(IDENTIFIER_5_PUBLICATION_YEAR, dto1.getPublicationYear());
        assertEquals(IDENTIFIER_5_PUBLICATION_MONTH, dto1.getPublicationMonth());
        assertEquals(IDENTIFIER_5_PUBLICATION_DAY, dto1.getPublicationDay());
        final List<RelatedIdentifierDto> relatedIdentifiersDto = dto1.getRelatedIdentifiers();
        assertEquals(1, relatedIdentifiersDto.size());
        final RelatedIdentifierDto relatedIdentifierDto1 = relatedIdentifiersDto.get(0);
        assertEquals(RELATED_IDENTIFIER_5_TYPE_DTO, relatedIdentifierDto1.getType());
        assertEquals(RELATED_IDENTIFIER_5_RELATION_TYPE_DTO, relatedIdentifierDto1.getRelation());
        assertEquals(RELATED_IDENTIFIER_5_VALUE, relatedIdentifierDto1.getValue());
        /* open search database */
        final Optional<DatabaseDto> responseDto = databaseIdxRepository.findById(DATABASE_2_ID);
        assertTrue(responseDto.isPresent());
    }

    @Test
    public void create_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException, IdentifierNotFoundException {

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_1_DTO_REQUEST, USER_1_PRINCIPAL);
        assertNotNull(response.getTitles());
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
        final Optional<DatabaseDto> responseDto = databaseIdxRepository.findById(DATABASE_1_ID);
        assertTrue(responseDto.isPresent());
    }

    @Test
    public void create_noRelatedTitleDescription_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException, IdentifierNotFoundException {

        /* mock */
        containerRepository.saveAll(List.of(CONTAINER_3, CONTAINER_4));
        databaseRepository.saveAll(List.of(DATABASE_3, DATABASE_4));

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_7_DTO_REQUEST, USER_1_PRINCIPAL);
        assertNotNull(response.getTitles());
        assertEquals(0, response.getTitles().size());
        assertNotNull(response.getDescriptions());
        assertEquals(0, response.getDescriptions().size());
        assertNotNull(response.getCreators());
        assertEquals(1, response.getCreators().size());
        assertNotNull(response.getFunders());
        assertEquals(0, response.getFunders().size());
        /* open search database */
        final Optional<DatabaseDto> responseDto = databaseIdxRepository.findById(DATABASE_1_ID);
        assertTrue(responseDto.isPresent());
    }

    @Test
    public void create_subsetHasDatabaseIdentifier_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException, IdentifierNotFoundException {

        /* mock */
        when(storeService.findOne(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL))
                .thenReturn(QUERY_1);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_2_DTO_REQUEST, USER_1_PRINCIPAL);
        assertEquals(IDENTIFIER_2_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_2_DATABASE_ID, response.getDatabase().getId());
        assertEquals(IDENTIFIER_2_QUERY, response.getQuery());
        assertEquals(IDENTIFIER_2_QUERY_HASH, response.getQueryHash());
        assertEquals(IDENTIFIER_2_RESULT_HASH, response.getResultHash());
        assertEquals(0, response.getTitles().size());
        assertEquals(0, response.getDescriptions().size());
        /* open search database */
        final Optional<DatabaseDto> responseDto = databaseIdxRepository.findById(DATABASE_1_ID);
        assertTrue(responseDto.isPresent());
    }

    @Test
    public void create_viewIdentifier_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException, IdentifierNotFoundException {

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_3_DTO_REQUEST, USER_1_PRINCIPAL);
        assertEquals(IDENTIFIER_3_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_3_DATABASE_ID, response.getDatabase().getId());
        assertEquals(IDENTIFIER_3_QUERY, response.getQuery());
        assertEquals(IDENTIFIER_3_QUERY_HASH, response.getQueryHash());
        assertEquals(IDENTIFIER_3_RESULT_HASH, response.getResultHash());
        assertEquals(0, response.getTitles().size());
        assertEquals(0, response.getDescriptions().size());
        assertEquals(1, response.getLicenses().size());
        /* open search database */
        final Optional<DatabaseDto> responseDto = databaseIdxRepository.findById(DATABASE_1_ID);
        assertTrue(responseDto.isPresent());
    }

    @Test
    @Transactional
    public void delete_succeeds() throws IdentifierNotFoundException, DatabaseNotFoundException {

        /* test */
        identifierService.delete(IDENTIFIER_1_ID);
        assertFalse(identifierRepository.findById(IDENTIFIER_1_ID).isPresent());
        /* open search database */
        final Optional<DatabaseDto> responseDto = databaseIdxRepository.findById(DATABASE_1_ID);
        assertTrue(responseDto.isPresent());
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.delete(9999L);
        });
    }

}
