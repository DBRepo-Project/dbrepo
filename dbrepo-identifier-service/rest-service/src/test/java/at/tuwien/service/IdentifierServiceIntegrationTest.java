package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierDescription;
import at.tuwien.entities.identifier.IdentifierTitle;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
public class IdentifierServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private IdentifierIdxRepository identifierIdxRepository;

    @MockBean
    private QueryServiceGateway queryServiceGateway;

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private IdentifierService identifierService;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private IdentifierTitleRepository identifierTitleRepository;

    @Autowired
    private IdentifierDescriptionRepository identifierDescriptionRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1_SIMPLE);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        userRepository.save(USER_2_SIMPLE);
        userRepository.save(USER_3_SIMPLE);
        userRepository.save(USER_4_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);
        databaseRepository.save(DATABASE_2_SIMPLE);
    }

    @Test
    public void create_subsetRelatedIdentifiers_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException,
            IdentifierRequestException {
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.ok(QUERY_2_DTO));
        when(queryServiceGateway.find(DATABASE_2_ID, IDENTIFIER_2_DTO_REQUEST, bearer))
                .thenReturn(QUERY_2_DTO);
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_2_DTO);
        identifierRepository.save(IDENTIFIER_1_SIMPLE);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_2_DTO_REQUEST, USER_2_PRINCIPAL, bearer);
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
        assertEquals(IDENTIFIER_2_DOI, response.getDoi());
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
    }

    @Test
    public void create_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException {
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_1_DTO);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_1_DTO_REQUEST, USER_1_PRINCIPAL, bearer);
    }

    @Test
    public void create_noRelatedTitleDescription_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException {
        final String bearer = "Bearer abcxyz";

        /* mock */
        containerRepository.save(CONTAINER_3_SIMPLE);
        containerRepository.save(CONTAINER_4_SIMPLE);
        databaseRepository.save(DATABASE_3_SIMPLE);
        databaseRepository.save(DATABASE_4_SIMPLE);
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_4_DTO);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_4_DTO_REQUEST, USER_1_PRINCIPAL, bearer);
    }

    @Test
    public void create_subsetHasDatabaseIdentifier_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            IdentifierAlreadyExistsException, QueryNotFoundException, IdentifierPublishingNotAllowedException,
            RemoteUnavailableException, IdentifierRequestException {
        final String authorization = "Bearer abcxyz";

        /* mock */
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
        when(queryServiceGateway.find(DATABASE_1_ID, IDENTIFIER_5_DTO_REQUEST, authorization))
                .thenReturn(QUERY_1_DTO);
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_5_DTO);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_5_DTO_REQUEST, USER_1_PRINCIPAL, authorization);
        assertEquals(IDENTIFIER_5_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_5_DATABASE_ID, response.getDatabase().getId());
        assertEquals(IDENTIFIER_5_QUERY, response.getQuery());
        assertEquals(IDENTIFIER_5_QUERY_HASH, response.getQueryHash());
        assertEquals(IDENTIFIER_5_RESULT_HASH, response.getResultHash());
        assertEquals(0, response.getTitles().size());
        assertEquals(0, response.getDescriptions().size());
    }

    @Test
    public void find_fails() {

        /* mock */
        identifierRepository.save(IDENTIFIER_1_SIMPLE);

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.find(IDENTIFIER_2_ID);
        });
    }

    @Test
    public void update_database_succeeds() throws UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierRequestException,
            IdentifierNotFoundException {

        /* mock */
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_1_DTO);

        /* test */
        final Identifier response = identifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST, USER_1_PRINCIPAL, "Bearer abc");
        assertEquals(IDENTIFIER_1_ID, response.getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, response.getDatabase().getId());
        assertEquals(1, response.getTitles().size());
        assertEquals(IDENTIFIER_1_TITLE_1_TITLE_MODIFY, response.getTitles().get(0).getTitle());
        assertEquals(IDENTIFIER_1_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_1_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_1_PUBLICATION_DAY, response.getPublicationDay());
    }

    @Test
    public void update_subset_succeeds() throws UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierRequestException,
            IdentifierNotFoundException {

        /* mock */
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_2_SIMPLE);
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_2_DTO);
        when(queryServiceGateway.find(eq(IDENTIFIER_2_DATABASE_ID), any(IdentifierSaveDto.class), anyString()))
                .thenReturn(QUERY_2_DTO);

        /* test */
        final Identifier response = identifierService.update(IDENTIFIER_2_ID, IDENTIFIER_2_DTO_UPDATE_REQUEST, USER_2_PRINCIPAL, "Bearer abc");
        assertEquals(IDENTIFIER_2_ID, response.getId());
        assertEquals(IDENTIFIER_2_DATABASE_ID, response.getDatabase().getId());
        assertEquals(1, response.getTitles().size());
        assertEquals(1, identifierRepository.findAll().stream().map(Identifier::getTitles).flatMap(List::stream).toList().size());
        assertEquals(IDENTIFIER_2_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_2_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_2_PUBLICATION_DAY, response.getPublicationDay());
    }

    @Test
    public void delete_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierIdxRepository.existsById(IDENTIFIER_1_ID))
                .thenReturn(true);
        doNothing()
                .when(identifierIdxRepository)
                .deleteById(IDENTIFIER_1_ID);
        identifierRepository.save(IDENTIFIER_1_SIMPLE);

        /* test */
        identifierService.delete(IDENTIFIER_1_ID);
        assertTrue(userRepository.findById(IDENTIFIER_1_CREATED_BY).isPresent()) /* no cascade of delete */;
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
