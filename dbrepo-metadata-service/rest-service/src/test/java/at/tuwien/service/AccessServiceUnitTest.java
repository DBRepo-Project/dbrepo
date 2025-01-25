package at.tuwien.service;

import at.tuwien.exception.*;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.repository.DatabaseRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AccessServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    @Qualifier("dataServiceRestTemplate")
    private RestTemplate dataServiceRestTemplate;

    @MockBean
    @Qualifier("searchServiceRestTemplate")
    private RestTemplate searchServiceRestTemplate;

    @Autowired
    private AccessService accessService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void list_succeeds() {

        /* test */
        accessService.list(DATABASE_1);
    }

    @Test
    public void find_succeeds() throws AccessNotFoundException {

        /* test */
        final DatabaseAccess response = accessService.find(DATABASE_1, USER_1);
        assertEquals(AccessType.READ, response.getType());
    }

    @Test
    public void create_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());
        when(searchServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
    }

    @Test
    public void create_dataService400_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void create_dataService403_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void create_dataService404_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void create_dataService500_fails() {

        /* mock */
        doThrow(HttpServerErrorException.InternalServerError.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void create_searchService400_fails() {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void create_searchService403_fails() {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void create_searchService404_fails() {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());
        doThrow(HttpClientErrorException.NotFound.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void create_searchService500_fails() {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());
        doThrow(HttpServerErrorException.InternalServerError.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            accessService.create(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void update_succeeds() throws DataServiceException, DataServiceConnectionException, AccessNotFoundException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        when(searchServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
    }

    @Test
    public void update_dataService400_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void update_dataService403_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void update_dataService404_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void update_dataService500_fails() {

        /* mock */
        doThrow(HttpServerErrorException.InternalServerError.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void update_searchService400_fails() {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void update_searchService403_fails() {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void update_searchService404_fails() {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        doThrow(HttpClientErrorException.NotFound.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void update_searchService500_fails() {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        doThrow(HttpServerErrorException.InternalServerError.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            accessService.update(DATABASE_1, USER_1, AccessTypeDto.WRITE_ALL);
        });
    }

    @Test
    public void delete_succeeds() throws DataServiceException, DataServiceConnectionException, AccessNotFoundException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        when(searchServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        accessService.delete(DATABASE_1, USER_1);
    }

    @Test
    public void delete_dataService403_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            accessService.delete(DATABASE_1, USER_1);
        });
    }

    @Test
    public void delete_dataService404_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            accessService.delete(DATABASE_1, USER_1);
        });
    }

    @Test
    public void delete_dataService500_fails() {

        /* mock */
        doThrow(HttpServerErrorException.InternalServerError.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            accessService.delete(DATABASE_1, USER_1);
        });
    }

    @Test
    public void delete_searchService400_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            accessService.delete(DATABASE_1, USER_1);
        });
    }

    @Test
    public void delete_searchService403_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            accessService.delete(DATABASE_1, USER_1);
        });
    }

    @Test
    public void delete_searchService404_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        doThrow(HttpClientErrorException.NotFound.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessService.delete(DATABASE_1, USER_1);
        });
    }

    @Test
    public void delete_searchService500_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());
        doThrow(HttpServerErrorException.InternalServerError.class)
                .when(searchServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            accessService.delete(DATABASE_1, USER_1);
        });
    }

}
