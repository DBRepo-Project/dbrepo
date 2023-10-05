package at.tuwien.gateway;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostModificationException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
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
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class BrokerServiceGatewayTest extends BaseUnitTest {

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

}
