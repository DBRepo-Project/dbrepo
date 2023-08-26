package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.datacite.DataCiteBody;
import at.tuwien.api.datacite.DataCiteData;
import at.tuwien.api.datacite.doi.DataCiteDoi;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.config.DataCiteConfig;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
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
@SpringBootTest(properties = "spring.profiles.active:local,doi")
@MockAmqp
@MockOpensearch
public class DataCiteIdentifierServiceUnitTest extends BaseUnitTest {

    @MockBean(answer = Answers.RETURNS_MOCKS)
    private DataCiteConfig dataCiteConfig;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    private EndpointConfig endpointConfig;

    @Autowired
    private LicenseRepository licenseRepository;

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
        licenseRepository.save(LICENSE_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
    }

    @Test
    public void create_database_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException,
            IdentifierRequestException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final DataCiteBody<DataCiteDoi> response =
                new DataCiteBody<>(new DataCiteData<>(null, "dois", new DataCiteDoi(IDENTIFIER_1_DOI_NOT_NULL)));

        /* mock */
        when(identifierService.create(any(IdentifierSaveDto.class), eq(principal)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(response));
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        /* test */
        Identifier result = dataCiteIdentifierService.create(IDENTIFIER_1_DTO_REQUEST, principal);
        assertTrue(identifierRepository.existsById(result.getId()));
        assertEquals(IDENTIFIER_1_DOI_NOT_NULL, result.getDoi());
    }

    @Test
    public void create_invalidMetadata_fails()
            throws IdentifierAlreadyExistsException, UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(identifierService.create(any(IdentifierSaveDto.class), eq(principal)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            dataCiteIdentifierService.create(IDENTIFIER_1_DTO_REQUEST, principal);
        });
        assertEquals(0, identifierRepository.count());
    }

    @Test
    public void create_restClientException_fails()
            throws IdentifierAlreadyExistsException, UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierPublishingNotAllowedException,
            IdentifierRequestException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(identifierService.create(any(IdentifierSaveDto.class), eq(principal)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(RestClientException.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        /* test */
        assertThrows(InternalError.class, () -> {
            dataCiteIdentifierService.create(IDENTIFIER_1_DTO_REQUEST, principal);
        });
        assertEquals(0, identifierRepository.count());
    }

    @Test
    public void update_existing_succeeds() throws IdentifierRequestException, UserNotFoundException,
            QueryNotFoundException, DatabaseNotFoundException, RemoteUnavailableException, IdentifierNotFoundException,
            QueryStoreException, DatabaseConnectionException, ImageNotSupportedException {
        final DataCiteBody<DataCiteDoi> response =
                new DataCiteBody<>(new DataCiteData<>(null, "dois", new DataCiteDoi(IDENTIFIER_1_DOI_NOT_NULL)));

        /* mock */
        when(identifierService.update(eq(IDENTIFIER_1_ID), any(IdentifierSaveDto.class), any(Principal.class)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1_WITH_DOI));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
                any(ParameterizedTypeReference.class), eq(IDENTIFIER_1_DOI_NOT_NULL)))
                .thenReturn(ResponseEntity.ok(response));
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        /* test */
        Identifier result = dataCiteIdentifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST, USER_1_PRINCIPAL);
        assertTrue(identifierRepository.existsById(IDENTIFIER_1_ID));
        assertEquals(IDENTIFIER_1_DOI_NOT_NULL, result.getDoi());
    }

    @Test
    public void update_invalidMetadata_fails() throws UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException {

        /* mock */
        when(identifierService.update(eq(IDENTIFIER_1_ID), any(IdentifierSaveDto.class), any(Principal.class)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1_WITH_DOI));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
                any(ParameterizedTypeReference.class), eq(IDENTIFIER_1_DOI_NOT_NULL)))
                .thenThrow(HttpClientErrorException.BadRequest.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        /* test */
        assertThrows(IdentifierRequestException.class, () -> {
            dataCiteIdentifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST, USER_1_PRINCIPAL);
        });
        assertEquals(0, identifierRepository.count());
    }

    @Test
    public void update_restClientException_fails() throws UserNotFoundException, QueryNotFoundException,
            DatabaseNotFoundException, RemoteUnavailableException, IdentifierNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException {

        /* mock */
        when(identifierService.update(eq(IDENTIFIER_1_ID), any(IdentifierSaveDto.class), any(Principal.class)))
                .thenAnswer((i) -> identifierRepository.save(IDENTIFIER_1_WITH_DOI));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
                any(ParameterizedTypeReference.class), eq(IDENTIFIER_1_DOI_NOT_NULL)))
                .thenThrow(RestClientException.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        /* test */
        assertThrows(InternalError.class, () -> {
            dataCiteIdentifierService.update(IDENTIFIER_1_ID, IDENTIFIER_1_DTO_UPDATE_REQUEST, USER_1_PRINCIPAL);
        });
        assertEquals(0, identifierRepository.count());
    }

}
