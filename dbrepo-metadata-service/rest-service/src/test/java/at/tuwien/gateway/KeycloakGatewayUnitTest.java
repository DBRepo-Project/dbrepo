package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.exception.AuthServiceConnectionException;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.impl.KeycloakGatewayImpl;
import at.tuwien.test.AbstractUnitTest;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class KeycloakGatewayUnitTest extends AbstractUnitTest {

    @MockBean
    @Qualifier("keycloakRestTemplate")
    private RestTemplate keycloakRestTemplate;

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private KeycloakGatewayImpl keycloakGateway;

    @Test
    public void deleteUser_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(keycloakRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(AuthServiceException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void deleteUser_succeeds() throws UserNotFoundException, AuthServiceException,
            AuthServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(keycloakRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        keycloakGateway.deleteUser(USER_1_ID);
    }

    @Test
    public void deleteUser_notFound_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.NotFound.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void deleteUser_unexpected_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.Conflict.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(AuthServiceException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void deleteUser_connection_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(AuthServiceConnectionException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void updateUserCredentials_succeeds() throws AuthServiceException, AuthServiceConnectionException,
            UserNotFoundException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(keycloakRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        keycloakGateway.updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);
    }

    @Test
    public void updateUserCredentials_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(keycloakRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(AuthServiceException.class, () -> {
            keycloakGateway.updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);
        });
    }

    @Test
    public void updateUserCredentials_connection_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(AuthServiceConnectionException.class, () -> {
            keycloakGateway.updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);
        });
    }

    @Test
    public void updateUserCredentials_unexpected_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.Conflict.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(AuthServiceException.class, () -> {
            keycloakGateway.updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);
        });
    }

}
