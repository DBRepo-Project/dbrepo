package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.VisibilityTypeDto;
import at.tuwien.config.IndexInitializer;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.RelatedIdentifier;
import at.tuwien.entities.identifier.VisibilityType;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.repository.elastic.IdentifierIdxRepository;
import at.tuwien.repository.jpa.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
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
    private IndexInitializer indexInitializer;

    @MockBean
    private IdentifierIdxRepository identifierIdxRepository;

    @MockBean
    private QueryServiceGateway queryServiceGateway;

    @MockBean
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

    @BeforeEach
    public void beforeEach() {
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        containerRepository.save(CONTAINER_2);
        databaseRepository.save(DATABASE_2);
        identifierRepository.save(IDENTIFIER_1);
    }

    @Test
    public void create_subsetRelatedIdentifiers_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException {
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.ok(QUERY_2_DTO));
        when(queryServiceGateway.find(CONTAINER_2_ID, DATABASE_2_ID, IDENTIFIER_2_DTO_REQUEST, bearer))
                .thenReturn(QUERY_2_DTO);
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_2_DTO);

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
    public void update_notFound_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.update(IDENTIFIER_2_ID, IDENTIFIER_1_DTO);
        });
    }

    @Test
    @Transactional(readOnly = true)
    public void update_succeeds() throws IdentifierNotFoundException, IdentifierPublishingNotAllowedException {

        /* mock */
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_1_DTO);

        /* test */
        final Identifier response = identifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO);
        assertEquals(IDENTIFIER_1_ID, response.getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_1_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_1_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_1_PUBLICATION_DAY, response.getPublicationDay());
    }

    @Test
    @Disabled("Multiple representation of the same entity")
    public void publish_everyone_succeeds() throws IdentifierAlreadyPublishedException, IdentifierNotFoundException {

        /* mock */
        identifierRepository.save(IDENTIFIER_1);
        identifierRepository.save(IDENTIFIER_2);

        /* mock */
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_2_DTO);

        /* test */
        final Identifier response = identifierService.publish(IDENTIFIER_2_ID, VisibilityTypeDto.EVERYONE);
        assertEquals(IDENTIFIER_2_ID, response.getId());
        assertEquals(IDENTIFIER_2_TITLE, response.getTitle());
        assertEquals(IDENTIFIER_2_DESCRIPTION, response.getDescription());
        assertEquals(IDENTIFIER_2_DOI, response.getDoi());
        assertEquals(IDENTIFIER_2_PUBLISHER, response.getPublisher());
        assertEquals(IDENTIFIER_2_CONTAINER_ID, response.getContainerId());
        assertEquals(IDENTIFIER_2_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_2_PUBLICATION_YEAR, response.getPublicationYear());
        assertEquals(IDENTIFIER_2_PUBLICATION_MONTH, response.getPublicationMonth());
        assertEquals(IDENTIFIER_2_PUBLICATION_DAY, response.getPublicationDay());
        assertEquals(VisibilityType.EVERYONE, response.getVisibility());
    }

    @Test
    @Disabled("Constraint identifier")
    public void publish_trusted_succeeds() throws IdentifierAlreadyPublishedException, IdentifierNotFoundException {

        /* mock */
        when(identifierIdxRepository.save(any(IdentifierDto.class)))
                .thenReturn(IDENTIFIER_2_DTO);

        /* test */
        identifierService.publish(IDENTIFIER_2_ID, VisibilityTypeDto.TRUSTED);
    }

    @Test
    public void delete_succeeds() throws IdentifierNotFoundException {

        /* mock */
        doNothing()
                .when(identifierIdxRepository)
                        .deleteById(IDENTIFIER_1_ID);

        /* test */
        identifierService.delete(IDENTIFIER_1_ID);
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.publish(IDENTIFIER_2_ID, VisibilityTypeDto.EVERYONE);
        });
    }

}
