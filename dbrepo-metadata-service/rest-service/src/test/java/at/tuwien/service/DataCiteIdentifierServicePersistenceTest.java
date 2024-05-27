package at.tuwien.service;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.repository.*;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.datacite.DataCiteBody;
import at.tuwien.api.datacite.doi.DataCiteDoi;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.gateway.SearchServiceGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(properties = "spring.profiles.active:local,doi")
public class DataCiteIdentifierServicePersistenceTest extends AbstractUnitTest {

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @MockBean
    @Qualifier("dataCiteRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private IdentifierService dataCiteIdentifierService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    private final ParameterizedTypeReference<DataCiteBody<DataCiteDoi>> dataCiteBodyParameterizedTypeReference = new ParameterizedTypeReference<>() {
    };

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4));
        databaseRepository.save(DATABASE_1);
    }

    @Test
    @Disabled
    public void save_database_succeeds() throws ServiceException, ServiceConnectionException,
            DatabaseNotFoundException, MalformedException, IdentifierNotFoundException, ViewNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException {
        final ResponseEntity<DataCiteBody<DataCiteDoi>> mock = ResponseEntity.status(HttpStatus.CREATED)
                .body(IDENTIFIER_1_DATA_CITE);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference)))
                .thenReturn(mock);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        dataCiteIdentifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
    }

    @Test
    @Disabled
    public void save_invalidMetadata_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference));
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        assertThrows(MalformedException.class, () -> {
            dataCiteIdentifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
        });
    }

    @Test
    @Disabled
    public void save_restClientException_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference));
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            dataCiteIdentifierService.save(DATABASE_1, USER_1, IDENTIFIER_1_SAVE_DTO);
        });
    }

    @Test
    @Disabled
    public void create_succeeds() throws SearchServiceException, MalformedException, ServiceException,
            QueryNotFoundException, ServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException {
        final ResponseEntity<DataCiteBody<DataCiteDoi>> mock = ResponseEntity.status(HttpStatus.CREATED)
                .body(IDENTIFIER_1_DATA_CITE);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference)))
                .thenReturn(mock);

        /* test */
        final Identifier response = dataCiteIdentifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_DTO);
        assertNotNull(response.getDoi());
    }

    @Test
    @Disabled
    public void create_hasDoi_succeeds() throws SearchServiceException, MalformedException, ServiceException,
            QueryNotFoundException, ServiceConnectionException, DatabaseNotFoundException,
            SearchServiceConnectionException, IdentifierNotFoundException, ViewNotFoundException {
        final ResponseEntity<DataCiteBody<DataCiteDoi>> mock = ResponseEntity.status(HttpStatus.CREATED)
                .body(IDENTIFIER_1_DATA_CITE);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(dataCiteBodyParameterizedTypeReference)))
                .thenReturn(mock);

        /* test */
        final Identifier response = dataCiteIdentifierService.create(DATABASE_1, USER_1, IDENTIFIER_1_CREATE_WITH_DOI_DTO);
        assertEquals(IDENTIFIER_1_DOI_NOT_NULL, response.getDoi());
    }

}
