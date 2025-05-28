package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SearchServiceGatewayUnitTest extends BaseTest {

    @MockBean
    @Qualifier("searchServiceRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private SearchServiceGateway searchServiceGateway;

    @Test
    public void update_succeeds() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        final ResponseEntity<DatabaseBriefDto> mock = ResponseEntity.accepted()
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class)))
                .thenReturn(mock);

        /* test */
        searchServiceGateway.update(DATABASE_1);
    }

    @Test
    public void update_badRequest_fails() {
        final ResponseEntity<DatabaseBriefDto> mock = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            searchServiceGateway.update(DATABASE_1);
        });
    }

    @Test
    public void update_unexpectedResponse_fails() {
        final ResponseEntity<DatabaseBriefDto> mock = ResponseEntity.status(HttpStatus.OK)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            searchServiceGateway.update(DATABASE_1);
        });
    }

    @Test
    public void update_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.ServiceUnavailable.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            searchServiceGateway.update(DATABASE_1);
        });
    }

    @Test
    public void update_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(DatabaseBriefDto.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            searchServiceGateway.update(DATABASE_1);
        });
    }

    @Test
    public void delete_succeeds() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        final ResponseEntity<Void> mock = ResponseEntity.accepted()
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        searchServiceGateway.delete(DATABASE_1_ID);
    }

    @Test
    public void delete_badRequest_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            searchServiceGateway.delete(DATABASE_1_ID);
        });
    }

    @Test
    public void delete_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            searchServiceGateway.delete(DATABASE_1_ID);
        });
    }

    @Test
    public void delete_unexpectedResponse_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.OK)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            searchServiceGateway.delete(DATABASE_1_ID);
        });
    }

    @Test
    public void delete_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.ServiceUnavailable.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            searchServiceGateway.delete(DATABASE_1_ID);
        });
    }

    @Test
    public void delete_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            searchServiceGateway.delete(DATABASE_1_ID);
        });
    }

}
