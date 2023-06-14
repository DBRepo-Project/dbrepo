package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.entities.identifier.Identifier;
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
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);
        databaseRepository.save(DATABASE_2_SIMPLE);
        identifierRepository.save(IDENTIFIER_1);
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
        when(queryServiceGateway.find(CONTAINER_2_ID, DATABASE_2_ID, IDENTIFIER_2_DTO_REQUEST, bearer))
                .thenReturn(QUERY_2_DTO);
        when(identifierIdxRepository.save(any(Identifier.class)))
                .thenReturn(IDENTIFIER_2);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_2_DTO_REQUEST, USER_2_PRINCIPAL, bearer);
        assertEquals(IDENTIFIER_2_ID, response.getId());
        assertEquals(IDENTIFIER_2_TITLE, response.getTitle());
        assertEquals(IDENTIFIER_2_DESCRIPTION, response.getDescription());
        assertEquals(IDENTIFIER_2_DOI, response.getDoi());
        assertEquals(IDENTIFIER_2_PUBLISHER, response.getPublisher());
        assertEquals(IDENTIFIER_2_CONTAINER_ID, response.getContainerId());
        assertEquals(IDENTIFIER_2_DATABASE_ID, response.getDatabaseId());
        assertNull(response.getLanguage());
        assertEquals(IDENTIFIER_2_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_2_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_2_PUBLICATION_DAY, response.getPublicationDay());
        final List<RelatedIdentifier> relatedIdentifiers = response.getRelated();
        assertEquals(1, relatedIdentifiers.size());
        final RelatedIdentifier relatedIdentifier1 = relatedIdentifiers.get(0);
        assertEquals(RELATED_IDENTIFIER_2_ID, relatedIdentifier1.getId());
        assertEquals(RELATED_IDENTIFIER_2_TYPE, relatedIdentifier1.getType());
        assertEquals(RELATED_IDENTIFIER_2_RELATION_TYPE, relatedIdentifier1.getRelation());
        assertEquals(RELATED_IDENTIFIER_2_VALUE, relatedIdentifier1.getValue());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.find(IDENTIFIER_2_ID);
        });
    }

    @Test
    public void update_succeeds()
            throws IdentifierNotFoundException, IdentifierRequestException {

        /* mock */
        when(identifierIdxRepository.save(any(Identifier.class)))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final Identifier response = identifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST);
        assertEquals(IDENTIFIER_1_ID, response.getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_1_TITLE_MODIFY, response.getTitle());
        assertEquals(IDENTIFIER_1_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_1_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_1_PUBLICATION_DAY, response.getPublicationDay());
    }

    @Test
    public void delete_succeeds() throws IdentifierNotFoundException {

        /* mock */
        when(identifierIdxRepository.existsById(IDENTIFIER_1_ID))
                .thenReturn(true);
        doNothing()
                .when(identifierIdxRepository)
                .deleteById(IDENTIFIER_1_ID);

        /* test */
        identifierService.delete(IDENTIFIER_1_ID);
        assertTrue(userRepository.findById(IDENTIFIER_1_CREATED_BY).isPresent()) /* no cascade of delete */;
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.delete(IDENTIFIER_2_ID);
        });
    }

}
