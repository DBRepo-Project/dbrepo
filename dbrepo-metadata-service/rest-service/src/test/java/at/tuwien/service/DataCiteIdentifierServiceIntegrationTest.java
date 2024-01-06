package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.datacite.DataCiteBody;
import at.tuwien.api.datacite.DataCiteData;
import at.tuwien.api.datacite.doi.DataCiteDoi;
import at.tuwien.config.DataCiteConfig;
import at.tuwien.config.EndpointConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
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
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(properties = "spring.profiles.active:local,doi")
@MockAmqp
@MockOpensearch
public class DataCiteIdentifierServiceIntegrationTest extends BaseUnitTest {

    @MockBean(answer = Answers.RETURNS_MOCKS)
    private DataCiteConfig dataCiteConfig;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    private EndpointConfig endpointConfig;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private UserRepository userRepository;

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

    @Autowired
    private IdentifierService dataCiteIdentifierService;

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
        licenseRepository.save(LICENSE_1);
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2));
    }

    @Test
    public void create_database_succeeds()
            throws DatabaseNotFoundException, UserNotFoundException, IdentifierAlreadyExistsException,
            QueryNotFoundException, IdentifierPublishingNotAllowedException, RemoteUnavailableException,
            IdentifierRequestException, ViewNotFoundException, QueryStoreException, DatabaseConnectionException,
            ImageNotSupportedException, IdentifierNotFoundException {
        final DataCiteBody<DataCiteDoi> response =
                new DataCiteBody<>(new DataCiteData<>(null, "dois", new DataCiteDoi(IDENTIFIER_1_DOI_NOT_NULL)));

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(response));
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        /* test */
        Identifier result = dataCiteIdentifierService.create(IDENTIFIER_1_DTO_REQUEST, USER_1_PRINCIPAL);
        assertTrue(identifierRepository.existsById(result.getId()));
        assertEquals(IDENTIFIER_1_DOI_NOT_NULL, result.getDoi());
    }

}
