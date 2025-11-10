package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.amqp.GrantExchangePermissionsDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.BrokerServiceException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class BrokerServiceGatewayUnitTest extends BaseTest {

    @MockitoBean
    @Qualifier("brokerRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private BrokerServiceGateway brokerServiceGateway;

    private final GrantExchangePermissionsDto WRITE_ALL_PERMISSIONS = GrantExchangePermissionsDto.builder()
            .exchange("dbrepo")
            .read("^(dbrepo\\." + DATABASE_1_ID + "\\..*)$") /* WRITE_ALL */
            .write("^(dbrepo\\." + DATABASE_1_ID + "\\..*)$")
            .build();

    @Test
    public void grantTopicPermission_exchangeNoRightsBefore_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.CREATED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
    }

    @Test
    public void grantTopicPermission_exchangeRightsSame_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
    }

    @Test
    public void grantTopicPermission_invalidResponseCode_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerServiceException.class, () -> {
            brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
        });
    }

    @Test
    public void grantVirtualHostPermission_virtualHostNoRightsBefore_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.CREATED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantVirtualHostPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
    }

    @Test
    public void grantVirtualHostPermission_virtualHostRightsSame_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantVirtualHostPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
    }

    @Test
    public void grantVirtualHostPermission_invalidResponseCode2_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.accepted()
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerServiceException.class, () -> {
            brokerServiceGateway.grantVirtualHostPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
        });
    }

    @Test
    public void grantVirtualHostPermission_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerServiceException.class, () -> {
            brokerServiceGateway.grantVirtualHostPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
        });
    }

    @Test
    public void grantVirtualHostPermission_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerServiceConnectionException.class, () -> {
            brokerServiceGateway.grantVirtualHostPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
        });
    }

    @Test
    public void grantTopicPermission_unexpected2_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerServiceException.class, () -> {
            brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
        });
    }

    @Test
    public void grantTopicPermission_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerServiceConnectionException.class, () -> {
            brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
        });
    }

    @Test
    public void grantExchangePermission_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());

        /* test */
        brokerServiceGateway.grantExchangePermission(USER_1_USERNAME, WRITE_ALL_PERMISSIONS);
    }

    @Test
    public void grantExchangePermission_exists_succeeds() throws BrokerServiceException,
            BrokerServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        brokerServiceGateway.grantExchangePermission(USER_1_USERNAME, WRITE_ALL_PERMISSIONS);
    }

    @Test
    public void grantExchangePermission_unexpected2_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .build());

        /* test */
        assertThrows(BrokerServiceException.class, () -> {
            brokerServiceGateway.grantExchangePermission(USER_1_USERNAME, WRITE_ALL_PERMISSIONS);
        });
    }

    @Test
    public void grantExchangePermission_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerServiceConnectionException.class, () -> {
            brokerServiceGateway.grantExchangePermission(USER_1_USERNAME, WRITE_ALL_PERMISSIONS);
        });
    }

    @Test
    public void grantExchangePermission_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerServiceException.class, () -> {
            brokerServiceGateway.grantExchangePermission(USER_1_USERNAME, WRITE_ALL_PERMISSIONS);
        });
    }

}
