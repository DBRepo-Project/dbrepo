package at.tuwien.gateway;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.amqp.UserDetailsDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.BrokerUserCreationException;
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

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class BrokerServiceGatewayUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    @Qualifier("gatewayRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private BrokerServiceGateway brokerServiceGateway;

    @Test
    public void createUser_succeeds() throws BrokerUserCreationException {
        final CreateUserDto request = CreateUserDto.builder()
                .password(USER_1_PASSWORD)
                .build();
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.CREATED)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.createUser(USER_1_USERNAME, request);
    }

    @Test
    public void createUser_fails() {
        final CreateUserDto request = CreateUserDto.builder()
                .password(USER_1_PASSWORD)
                .build();
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerUserCreationException.class, () -> {
            brokerServiceGateway.createUser(USER_1_USERNAME, request);
        });
    }

    @Test
    public void findUser_succeeds() throws BrokerUserCreationException {
        final ResponseEntity<UserDetailsDto> mock = ResponseEntity.ok()
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDetailsDto.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.findUser(USER_1_USERNAME);
    }

    @Test
    public void findUser_fails() {
        final ResponseEntity<UserDetailsDto> mock = ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDetailsDto.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerUserCreationException.class, () -> {
            brokerServiceGateway.findUser(USER_1_USERNAME);
        });
    }

    @Test
    public void modifyHostPermissions_succeeds() throws BrokerUserCreationException {
        final GrantVirtualHostPermissionsDto request = GrantVirtualHostPermissionsDto.builder()
                .read(".*")
                .write(".*")
                .configure(".*")
                .build();
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.CREATED)
                .build();

        /* mock */
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.modifyHostPermissions(USER_1_USERNAME, request);
    }

    @Test
    public void modifyHostPermissions_fails() {
        final GrantVirtualHostPermissionsDto request = GrantVirtualHostPermissionsDto.builder()
                .read(".*")
                .write(".*")
                .configure(".*")
                .build();
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build();

        /* mock */
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerUserCreationException.class, () -> {
            brokerServiceGateway.modifyHostPermissions(USER_1_USERNAME, request);
        });
    }

    @Test
    public void modifyUserPassword_succeeds() throws BrokerUserCreationException {
        final CreateUserDto request = CreateUserDto.builder()
                .password(USER_1_PASSWORD)
                .build();
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        brokerServiceGateway.modifyUserPassword(USER_1_USERNAME, request);
    }

    @Test
    public void modifyUserPassword_fails() {
        final CreateUserDto request = CreateUserDto.builder()
                .password(USER_1_PASSWORD)
                .build();
        final ResponseEntity<Void> mock = ResponseEntity.status(HttpStatus.OK)
                .build();

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(mock);

        /* test */
        assertThrows(BrokerUserCreationException.class, () -> {
            brokerServiceGateway.modifyUserPassword(USER_1_USERNAME, request);
        });
    }

}
