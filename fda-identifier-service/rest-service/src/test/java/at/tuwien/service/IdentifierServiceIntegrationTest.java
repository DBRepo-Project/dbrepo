package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.VisibilityTypeDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.gateway.QueryServiceGateway;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.impl.IdentifierServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class IdentifierServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private IdentifierServiceImpl identifierService;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private CreatorRepository creatorRepository;

    @MockBean
    private QueryServiceGateway queryServiceGateway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @BeforeEach
    @Transactional
    public void beforeEach() {
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        identifierRepository.save(IDENTIFIER_1);
        creatorRepository.save(CREATOR_1);
        creatorRepository.save(CREATOR_2);
        IDENTIFIER_1.setCreators(List.of(CREATOR_1, CREATOR_2));
        identifierRepository.save(IDENTIFIER_1);
        containerRepository.save(CONTAINER_2);
        databaseRepository.save(DATABASE_2);
    }

    @Test
    @Transactional
    public void findAll_succeeds() {

        /* mock */
        identifierRepository.save(Identifier.builder()
                .id(IDENTIFIER_2_ID)
                .containerId(IDENTIFIER_2_CONTAINER_ID)
                .queryId(IDENTIFIER_2_QUERY_ID)
                .databaseId(IDENTIFIER_2_DATABASE_ID)
                .description(IDENTIFIER_2_DESCRIPTION)
                .title(IDENTIFIER_2_TITLE)
                .doi(IDENTIFIER_2_DOI)
                .visibility(IDENTIFIER_2_VISIBILITY)
                .created(IDENTIFIER_2_CREATED)
                .lastModified(IDENTIFIER_2_MODIFIED)
                .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
                .publisher(IDENTIFIER_2_PUBLISHER)
                .type(IDENTIFIER_2_TYPE)
                .build());

        /* test */
        final List<Identifier> response = identifierService.findAll(CONTAINER_1_ID, DATABASE_1_ID);
        assertEquals(1, response.size());
        assertEquals(IDENTIFIER_1_ID, response.get(0).getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, response.get(0).getDatabaseId());
        assertEquals(IDENTIFIER_1_QUERY_ID, response.get(0).getQueryId());
        assertEquals(IDENTIFIER_1_TITLE, response.get(0).getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, response.get(0).getDescription());
        assertEquals(IDENTIFIER_1_DOI, response.get(0).getDoi());
        assertEquals(2, response.get(0).getCreators().size());
        assertEquals(CREATOR_1_ID, response.get(0).getCreators().get(0).getId());
        assertEquals(CREATOR_2_ID, response.get(0).getCreators().get(1).getId());
        assertEquals(IDENTIFIER_1_PUBLISHER, response.get(0).getPublisher());
        assertEquals(IDENTIFIER_1_TYPE, response.get(0).getType());
    }

    @Test
    public void create_succeeds() throws IdentifierPublishingNotAllowedException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierAlreadyExistsException, UserNotFoundException,
            DatabaseNotFoundException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(queryServiceGateway.find(CONTAINER_2_ID, DATABASE_2_ID, IDENTIFIER_2_DTO_REQUEST, bearer))
                .thenReturn(QUERY_2_DTO);

        /* test */
        final Identifier response = identifierService.create(IDENTIFIER_2_DTO_REQUEST, principal, bearer);
        assertEquals(IDENTIFIER_2_ID, response.getId());
        assertEquals(IDENTIFIER_2_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_2_QUERY_ID, response.getQueryId());
        assertEquals(IDENTIFIER_2_DOI, response.getDoi());
        assertEquals(IDENTIFIER_2_TITLE, response.getTitle());
        assertEquals(IDENTIFIER_2_DESCRIPTION, response.getDescription());
        assertEquals(2, response.getCreators().size());
    }

    @Test
    public void create_queryNotExists_fails() throws QueryNotFoundException, RemoteUnavailableException {
        IdentifierCreateDto request = IdentifierCreateDto.builder()
                .qid(IDENTIFIER_2_QUERY_ID)
                .cid(IDENTIFIER_2_CONTAINER_ID)
                .dbid(IDENTIFIER_2_DATABASE_ID)
                .description(IDENTIFIER_2_DESCRIPTION)
                .title(IDENTIFIER_2_TITLE)
                .doi(IDENTIFIER_2_DOI)
                .visibility(IDENTIFIER_2_VISIBILITY_DTO)
                .publicationYear(IDENTIFIER_2_PUBLICATION_YEAR)
                .publisher(IDENTIFIER_2_PUBLISHER)
                .type(IDENTIFIER_2_TYPE_DTO)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final String bearer = "Bearer abcxyz";

        /* mock */
        doThrow(QueryNotFoundException.class)
                .when(queryServiceGateway)
                .find(CONTAINER_2_ID, DATABASE_2_ID, request, bearer);

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            identifierService.create(request, principal, bearer);
        });
    }

    @Test
    public void create_identifierAlreadyExists_fails() {
        IdentifierCreateDto request = IdentifierCreateDto.builder()
                .qid(IDENTIFIER_1_QUERY_ID)
                .cid(IDENTIFIER_1_CONTAINER_ID)
                .dbid(IDENTIFIER_1_DATABASE_ID)
                .description(IDENTIFIER_1_DESCRIPTION)
                .title(IDENTIFIER_1_TITLE)
                .doi(IDENTIFIER_1_DOI)
                .visibility(IDENTIFIER_1_VISIBILITY_DTO)
                .publicationYear(IDENTIFIER_1_PUBLICATION_YEAR)
                .publisher(IDENTIFIER_1_PUBLISHER)
                .type(IDENTIFIER_1_TYPE_DTO)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final String bearer = "Bearer abcxyz";

        /* mock */

        /* test */
        assertThrows(IdentifierAlreadyExistsException.class, () -> {
            identifierService.create(request, principal, bearer);
        });
    }

    @Test
    public void create_queryServiceUnavailable_fails() throws QueryNotFoundException, RemoteUnavailableException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final String bearer = "Bearer abcxyz";

        /* mock */
        doThrow(RemoteUnavailableException.class)
                .when(queryServiceGateway)
                .find(CONTAINER_2_ID, DATABASE_2_ID, IDENTIFIER_2_DTO_REQUEST, bearer);

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            identifierService.create(IDENTIFIER_2_DTO_REQUEST, principal, bearer);
        });
    }

    @Test
    @Transactional
    public void find_succeeds() throws IdentifierNotFoundException {

        /* test */
        final Identifier response = identifierService.find(IDENTIFIER_1_ID);
        assertEquals(IDENTIFIER_1_ID, response.getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_1_QUERY_ID, response.getQueryId());
        assertEquals(IDENTIFIER_1_TITLE, response.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, response.getDescription());
        assertEquals(IDENTIFIER_1_DOI, response.getDoi());
        assertEquals(2, response.getCreators().size());
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
    public void update_succeeds() throws IdentifierNotFoundException {

        /* test */
        final Identifier response = identifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO);
        assertEquals(IDENTIFIER_1_ID, response.getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, response.getDatabaseId());
        assertEquals(IDENTIFIER_1_QUERY_ID, response.getQueryId());
        assertEquals(IDENTIFIER_1_TITLE, response.getTitle());
        assertEquals(IDENTIFIER_1_DESCRIPTION, response.getDescription());
        assertEquals(IDENTIFIER_1_DOI, response.getDoi());
        assertEquals(2, response.getCreators().size());
    }

    @Test
    public void publish_everyone_succeeds() throws IdentifierAlreadyPublishedException, IdentifierNotFoundException {
        identifierRepository.save(IDENTIFIER_2);

        /* test */
        identifierService.publish(IDENTIFIER_2_ID, VisibilityTypeDto.EVERYONE);
    }

    @Test
    public void publish_trusted_succeeds() throws IdentifierAlreadyPublishedException, IdentifierNotFoundException {
        identifierRepository.save(IDENTIFIER_2);

        /* test */
        identifierService.publish(IDENTIFIER_2_ID, VisibilityTypeDto.TRUSTED);
    }

    @Test
    public void publish_notFound_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.publish(IDENTIFIER_2_ID, VisibilityTypeDto.EVERYONE);
        });
    }

    @Test
    @Transactional
    public void publish_alreadyPublished_fails()
            throws IdentifierAlreadyPublishedException, IdentifierNotFoundException {
        identifierRepository.save(IDENTIFIER_2);

        /* mock */
        identifierService.publish(IDENTIFIER_2_ID, VisibilityTypeDto.EVERYONE);

        /* test */
        assertThrows(IdentifierAlreadyPublishedException.class, () -> {
            identifierService.publish(IDENTIFIER_2_ID, VisibilityTypeDto.EVERYONE);
        });
    }

    @Test
    public void delete_succeeds() throws IdentifierNotFoundException {

        /* test */
        identifierService.delete(IDENTIFIER_1_ID);
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.delete(IDENTIFIER_2_ID);
        });
    }

}
