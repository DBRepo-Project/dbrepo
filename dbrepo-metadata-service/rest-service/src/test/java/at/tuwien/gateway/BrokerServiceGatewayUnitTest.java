package at.tuwien.gateway;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.amqp.ExchangeDto;
import at.tuwien.api.amqp.QueueDto;
import at.tuwien.exception.*;
import lombok.extern.log4j.Log4j2;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class BrokerServiceGatewayUnitTest extends BaseUnitTest {

    @MockBean
    @Qualifier("brokerRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private BrokerServiceGateway brokerServiceGateway;

    @Test
    public void createVirtualHost_succeeds() throws BrokerVirtualHostModificationException, BrokerRemoteException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.CREATED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.createVirtualHost(VIRTUAL_HOST_CREATE_DTO);
    }

    @Test
    public void createVirtualHost_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerVirtualHostModificationException.class, () -> {
            brokerServiceGateway.createVirtualHost(VIRTUAL_HOST_CREATE_DTO);
        });
    }

    @Test
    public void createVirtualHost_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerRemoteException.class, () -> {
            brokerServiceGateway.createVirtualHost(VIRTUAL_HOST_CREATE_DTO);
        });
    }

    @Test
    public void grantPermission_exchangeNoRightsBefore_succeeds() throws BrokerVirtualHostGrantException, BrokerRemoteException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.CREATED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
    }

    @Test
    public void grantPermission_exchangeRightsSame_succeeds() throws BrokerVirtualHostGrantException, BrokerRemoteException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
    }

    @Test
    public void grantPermission_invalidResponseCode_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerVirtualHostGrantException.class, () -> {
            brokerServiceGateway.grantPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
        });
    }

    @Test
    public void grantPermission_virtualHostNoRightsBefore_succeeds() throws BrokerRemoteException, BrokerVirtualHostGrantException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.CREATED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
    }

    @Test
    public void grantPermission_virtualHostRightsSame_succeeds() throws BrokerRemoteException, BrokerVirtualHostGrantException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
    }

    @Test
    public void grantPermission_invalidResponseCode2_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerVirtualHostGrantException.class, () -> {
            brokerServiceGateway.grantPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
        });
    }

    @Test
    public void grantPermission_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerRemoteException.class, () -> {
            brokerServiceGateway.grantPermission(USER_1_USERNAME, VIRTUAL_HOST_GRANT_DTO);
        });
    }

    @Test
    public void grantPermission_unexpected2_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerRemoteException.class, () -> {
            brokerServiceGateway.grantPermission(USER_1_USERNAME, VIRTUAL_HOST_EXCHANGE_UPDATE_DTO);
        });
    }

    @Test
    public void createUser_succeeds() throws BrokerRemoteException, BrokerVirtualHostModificationException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.createUser(USER_1_USERNAME, USER_1_PASSWORD);
    }

    @Test
    public void createUser_invalidResponseCode_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.ACCEPTED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerVirtualHostModificationException.class, () -> {
            brokerServiceGateway.createUser(USER_1_USERNAME, USER_1_PASSWORD);
        });
    }

    @Test
    public void createUser_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerRemoteException.class, () -> {
            brokerServiceGateway.createUser(USER_1_USERNAME, USER_1_PASSWORD);
        });
    }

    @Test
    public void findQueue_fails() {
        final ResponseEntity<QueueDto> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QueueDto.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(QueueNotFoundException.class, () -> {
            brokerServiceGateway.findQueue("dbrepo");
        });
    }

    @Test
    public void findQueue_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QueueDto.class));

        /* test */
        assertThrows(BrokerRemoteException.class, () -> {
            brokerServiceGateway.findQueue("dbrepo");
        });
    }

    @Test
    public void findQueue_succeeds() throws QueueNotFoundException, BrokerRemoteException {
        final ResponseEntity<QueueDto> mock = ResponseEntity.status(HttpStatus.OK)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(QueueDto.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.findQueue("dbrepo");
    }

    @Test
    public void findExchange_fails() {
        final ResponseEntity<ExchangeDto> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ExchangeDto.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(ExchangeNotFoundException.class, () -> {
            brokerServiceGateway.findExchange("dbrepo");
        });
    }

    @Test
    public void findExchange_succeeds() throws BrokerRemoteException, ExchangeNotFoundException {
        final ResponseEntity<ExchangeDto> mock = ResponseEntity.status(HttpStatus.OK)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ExchangeDto.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.findExchange("dbrepo");
    }

    @Test
    public void findExchange_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ExchangeDto.class));

        /* test */
        assertThrows(BrokerRemoteException.class, () -> {
            brokerServiceGateway.findExchange("dbrepo");
        });
    }

    @Test
    public void deleteUser_succeeds() throws BrokerRemoteException, BrokerVirtualHostModificationException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.deleteUser(USER_1_USERNAME);
    }

    @Test
    public void deleteUser_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.OK)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerVirtualHostModificationException.class, () -> {
            brokerServiceGateway.deleteUser(USER_1_USERNAME);
        });
    }

    @Test
    public void deleteUser_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerRemoteException.class, () -> {
            brokerServiceGateway.deleteUser(USER_1_USERNAME);
        });
    }

    @Test
    public void grantTopicPermission_succeeds() throws BrokerRemoteException, BrokerVirtualHostGrantException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.CREATED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, USER_1_RABBITMQ_GRANT_TOPIC_DTO);
    }

    @Test
    public void grantTopicPermission_exists_succeeds() throws BrokerRemoteException, BrokerVirtualHostGrantException {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, USER_1_RABBITMQ_GRANT_TOPIC_DTO);
    }

    @Test
    public void grantTopicPermission_unexpected2_fails() {
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerVirtualHostGrantException.class, () -> {
            brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, USER_1_RABBITMQ_GRANT_TOPIC_DTO);
        });
    }

    @Test
    public void grantTopicPermission_unexpected_fails() {

        /* mock */
        doThrow(RestClientException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(BrokerRemoteException.class, () -> {
            brokerServiceGateway.grantTopicPermission(USER_1_USERNAME, USER_1_RABBITMQ_GRANT_TOPIC_DTO);
        });
    }

}
