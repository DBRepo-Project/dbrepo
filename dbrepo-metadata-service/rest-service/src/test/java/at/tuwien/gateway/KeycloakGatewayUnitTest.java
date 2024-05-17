package at.tuwien.gateway;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.impl.KeycloakGatewayImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class KeycloakGatewayUnitTest extends AbstractUnitTest {

    @MockBean
    @Qualifier("keycloakRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private KeycloakGatewayImpl keycloakGateway;

    @Test
    public void obtainToken_succeeds() throws ServiceException, ServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));

        /* test */
        keycloakGateway.obtainToken();
    }

    @Test
    public void obtainToken_noAccess_fails() {

        /* mock */
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            keycloakGateway.obtainToken();
        });
    }

    @Test
    public void obtainToken_fails() {

        /* mock */
        doThrow(HttpServerErrorException.BadGateway.create(HttpStatus.BAD_GATEWAY, "", new HttpHeaders(), new byte[]{}, Charset.defaultCharset()))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            keycloakGateway.obtainToken();
        });
    }

    @Test
    public void createUser_succeeds() throws UserExistsException, ServiceException, ServiceConnectionException, EmailExistsException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());

        /* test */
        keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
    }

    @Test
    public void createUser_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(ServiceException.class, () -> {
            keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        });
    }

    @Test
    public void createUser_sameEMail_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.Conflict.create(HttpStatus.CONFLICT, "same email", new HttpHeaders(), new byte[]{}, null))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(EmailExistsException.class, () -> {
            keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        });
    }

    @Test
    public void createUser_sameUsername_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.Conflict.create(HttpStatus.CONFLICT, "same username", new HttpHeaders(), new byte[]{}, null))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(UserExistsException.class, () -> {
            keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        });
    }

    @Test
    public void createUser_unexpected_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        });
    }

    @Test
    public void createUser_unexpected2_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.BadGateway.create(HttpStatus.BAD_GATEWAY, "", new HttpHeaders(), new byte[]{}, Charset.defaultCharset()))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        });
    }

    @Test
    public void deleteUser_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(ServiceException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void deleteUser_succeeds() throws ServiceException, ServiceConnectionException, UserNotFoundException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        keycloakGateway.deleteUser(USER_1_ID);
    }

    @Test
    public void deleteUser_unexpected_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void deleteUser_notFound_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "", new HttpHeaders(), new byte[]{}, Charset.defaultCharset()))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void deleteUser_unexpected2_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.BadGateway.create(HttpStatus.BAD_GATEWAY, "", new HttpHeaders(), new byte[]{}, Charset.defaultCharset()))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(ServiceException.class, () -> {
            keycloakGateway.deleteUser(USER_1_ID);
        });
    }

    @Test
    public void updateUserCredentials_succeeds() throws ServiceException, ServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
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
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(ServiceException.class, () -> {
            keycloakGateway.updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);
        });
    }

    @Test
    public void updateUserCredentials_unexpected_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            keycloakGateway.updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);
        });
    }

    @Test
    public void updateUserCredentials_unexpected2_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.BadGateway.create(HttpStatus.BAD_GATEWAY, "", new HttpHeaders(), new byte[]{}, Charset.defaultCharset()))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(ServiceException.class, () -> {
            keycloakGateway.updateUserCredentials(USER_1_ID, USER_1_PASSWORD_DTO);
        });
    }

    @Test
    public void findByUsername_notFound_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(new UserDto[]{}));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            keycloakGateway.findByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void findByUsername_remote_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto[].class));

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            keycloakGateway.findByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void findByUsername_unexpected_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.BadGateway.create(HttpStatus.BAD_GATEWAY, "", new HttpHeaders(), new byte[]{}, Charset.defaultCharset()))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto[].class));

        /* test */
        assertThrows(ServiceException.class, () -> {
            keycloakGateway.findByUsername(USER_1_USERNAME);
        });
    }

}
