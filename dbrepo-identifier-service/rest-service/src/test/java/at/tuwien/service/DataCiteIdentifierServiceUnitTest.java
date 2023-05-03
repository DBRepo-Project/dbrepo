package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.datacite.DataCiteBody;
import at.tuwien.api.datacite.DataCiteData;
import at.tuwien.api.datacite.doi.DataCiteDoi;
import at.tuwien.api.identifier.IdentifierCreateDto;
import at.tuwien.api.identifier.IdentifierUpdateDto;
import at.tuwien.config.DataCiteConfig;
import at.tuwien.config.EndpointConfig;
import at.tuwien.config.IndexInitializer;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.impl.IdentifierServiceImpl;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ActiveProfiles("doi")
public class DataCiteIdentifierServiceUnitTest extends BaseUnitTest {

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    private DataCiteConfig dataCiteConfig;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    private EndpointConfig endpointConfig;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @MockBean(answer = Answers.RETURNS_SELF)
    private RestTemplateBuilder restTemplateBuilder;

    @MockBean
    private IdentifierServiceImpl identifierService;

    @Autowired
    private IdentifierService dataCiteIdentifierService;

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    public void create_database_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException,
            IdentifierRequestException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final String bearer = "Bearer abcxyz";
        final DataCiteBody<DataCiteDoi> response =
                new DataCiteBody<>(new DataCiteData<>(null, "dois", new DataCiteDoi(IDENTIFIER_1_DOI_NOT_NULL)));

        /* mock */
        when(identifierService.create(any(IdentifierCreateDto.class), eq(principal), eq(bearer)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(response));

        /* test */
        Identifier result = dataCiteIdentifierService.create(IDENTIFIER_1_DTO_REQUEST, principal, bearer);
        assertTrue(identifierRepository.existsById(result.getId()));
        assertEquals(IDENTIFIER_1_DOI_NOT_NULL, result.getDoi());
    }

    @Test
    public void create_invalidMetadata_fails()
            throws IdentifierAlreadyExistsException, UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(identifierService.create(any(IdentifierCreateDto.class), eq(principal), eq(bearer)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            dataCiteIdentifierService.create(IDENTIFIER_1_DTO_REQUEST, principal, bearer);
        });
        assertEquals(0, identifierRepository.count());
    }

    @Test
    public void create_restClientException_fails()
            throws IdentifierAlreadyExistsException, UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final String bearer = "Bearer abcxyz";

        /* mock */
        when(identifierService.create(any(IdentifierCreateDto.class), eq(principal), eq(bearer)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(RestClientException.class);

        /* test */
        assertThrows(InternalError.class, () -> {
            dataCiteIdentifierService.create(IDENTIFIER_1_DTO_REQUEST, principal, bearer);
        });
        assertEquals(0, identifierRepository.count());
    }

    @Test
    public void update_existing_succeeds()
            throws IdentifierRequestException, IdentifierNotFoundException, IdentifierUpdateBadFormException {
        final DataCiteBody<DataCiteDoi> response =
                new DataCiteBody<>(new DataCiteData<>(null, "dois", new DataCiteDoi(IDENTIFIER_1_DOI_NOT_NULL)));

        /* mock */
        when(identifierService.update(eq(IDENTIFIER_1_ID), any(IdentifierUpdateDto.class)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1_WITH_DOI));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
                any(ParameterizedTypeReference.class), eq(IDENTIFIER_1_DOI_NOT_NULL)))
                .thenReturn(ResponseEntity.ok(response));

        /* test */
        Identifier result = dataCiteIdentifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST);
        assertTrue(identifierRepository.existsById(IDENTIFIER_1_ID));
        assertEquals(IDENTIFIER_1_DOI_NOT_NULL, result.getDoi());
    }

    @Test
    public void update_invalidMetadata_fails() throws IdentifierUpdateBadFormException, IdentifierNotFoundException {

        /* mock */
        when(identifierService.update(eq(IDENTIFIER_1_ID), any(IdentifierUpdateDto.class)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1_WITH_DOI));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
                any(ParameterizedTypeReference.class), eq(IDENTIFIER_1_DOI_NOT_NULL)))
                .thenThrow(HttpClientErrorException.BadRequest.class);

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            dataCiteIdentifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST);
        });
        assertEquals(0, identifierRepository.count());
    }

    @Test
    public void update_restClientException_fails() throws IdentifierUpdateBadFormException, IdentifierNotFoundException {

        /* mock */
        when(identifierService.update(eq(IDENTIFIER_1_ID), any(IdentifierUpdateDto.class)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1_WITH_DOI));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
                any(ParameterizedTypeReference.class), eq(IDENTIFIER_1_DOI_NOT_NULL)))
                .thenThrow(RestClientException.class);

        /* test */
        assertThrows(InternalError.class, () -> {
            dataCiteIdentifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST);
        });
        assertEquals(0, identifierRepository.count());
    }

}
