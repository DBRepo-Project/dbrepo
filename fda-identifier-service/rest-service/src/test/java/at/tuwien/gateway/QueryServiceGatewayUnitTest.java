package at.tuwien.gateway;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.QueryNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.repository.jpa.UserRepository;
import com.google.common.io.Files;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryServiceGatewayUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private QueryServiceGateway queryServiceGateway;

    @Test
    public void find_succeeds() throws QueryNotFoundException, RemoteUnavailableException {
        final ResponseEntity<QueryDto> mock = ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(QUERY_1_DTO);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(mock);

        /* test */
        final QueryDto response = queryServiceGateway.find(CONTAINER_1_ID, DATABASE_1_ID, IDENTIFIER_1_DTO_REQUEST, JWT_1);
        assertNotNull(response);
        assertEquals(QUERY_1_ID, response.getId());
    }

    @Test
    public void find_notFound_fails() throws QueryNotFoundException, RemoteUnavailableException {
        final ResponseEntity<QueryDto> mock = ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            queryServiceGateway.find(CONTAINER_1_ID, DATABASE_1_ID, IDENTIFIER_1_DTO_REQUEST, JWT_1);
        });
    }

    @Test
    public void find_notAvailable_fails() {
        final ResponseEntity<QueryDto> mock = ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();

        /* mock */
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QueryDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            queryServiceGateway.find(CONTAINER_1_ID, DATABASE_1_ID, IDENTIFIER_1_DTO_REQUEST, JWT_1);
        });
    }

    @Test
    public void find_notAuthorized_fails() {
        final ResponseEntity<QueryDto> mock = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QueryDto.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            queryServiceGateway.find(CONTAINER_1_ID, DATABASE_1_ID, IDENTIFIER_1_DTO_REQUEST, JWT_1);
        });
    }

    @Test
    public void export_succeeds() throws IOException, QueryNotFoundException, RemoteUnavailableException {
        final byte[] bytes = Files.toByteArray(new File("src/test/resources/csv/testdata.csv"));
        final ResponseEntity<byte[]> mock = ResponseEntity.status(HttpStatus.OK)
                .body(bytes);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mock);

        /* test */
        final byte[] response = queryServiceGateway.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID);
        assertNotNull(response);
        assertEquals(bytes, response);
    }

    @Test
    public void export_notFound_fails() {
        final ResponseEntity<byte[]> mock = ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mock);

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            queryServiceGateway.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void export_notAuthorized_fails() {
        final ResponseEntity<byte[]> mock = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mock);

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            queryServiceGateway.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void export_notAvailable_succeeds() {

        /* mock */
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            queryServiceGateway.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID);
        });
    }

}
