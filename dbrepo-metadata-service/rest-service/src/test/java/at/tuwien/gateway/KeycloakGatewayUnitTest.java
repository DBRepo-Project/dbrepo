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
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.*;
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
    public void createUser_succeeds() throws UserExistsException, EmailExistsException, AuthServiceException,
            AuthServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(keycloakRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
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
        when(keycloakRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(AuthServiceException.class, () -> {
            keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        });
    }

    @Test
    public void createUser_sameUsername_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.Conflict.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(UserExistsException.class, () -> {
            keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        });
    }

    @Test
    public void createUser_connection_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(AuthServiceConnectionException.class, () -> {
            keycloakGateway.createUser(USER_1_KEYCLOAK_SIGNUP_REQUEST);
        });
    }

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

    @Test
    public void findByUsername_notFound_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(keycloakRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(new UserDto[]{}));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            keycloakGateway.findByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void findByUsername_connection_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto[].class));

        /* test */
        assertThrows(AuthServiceConnectionException.class, () -> {
            keycloakGateway.findByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void findByUsername_unexpected_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.Conflict.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto[].class));

        /* test */
        assertThrows(AuthServiceException.class, () -> {
            keycloakGateway.findByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void findById_succeeds() throws UserNotFoundException, AuthServiceException, AuthServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        when(keycloakRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(USER_1_KEYCLOAK_DTO));

        /* test */
        final UserDto response = keycloakGateway.findById(USER_1_ID);
        assertEquals(USER_1_ID, response.getId());
    }

    @Test
    public void findById_notFound_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.NotFound.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto.class));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            keycloakGateway.findById(USER_1_ID);
        });
    }

    @Test
    public void findById_connection_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpServerErrorException.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto.class));

        /* test */
        assertThrows(AuthServiceConnectionException.class, () -> {
            keycloakGateway.findById(USER_1_ID);
        });
    }

    @Test
    public void findById_unexpected_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));
        doThrow(HttpClientErrorException.Conflict.class)
                .when(keycloakRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(UserDto.class));

        /* test */
        assertThrows(AuthServiceException.class, () -> {
            keycloakGateway.findById(USER_1_ID);
        });
    }

    @Test
    public void refreshUserToken_succeeds() throws AuthServiceConnectionException, CredentialsInvalidException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));

        /* test */
        final TokenDto response = keycloakGateway.refreshUserToken(TOKEN_DTO.getRefreshToken());
        assertNotNull(response.getAccessToken());
    }

    @Test
    public void refreshUserToken_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(AuthServiceConnectionException.class, () -> {
            keycloakGateway.refreshUserToken(TOKEN_DTO.getRefreshToken());
        });
    }

    @Test
    public void refreshUserToken_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(CredentialsInvalidException.class, () -> {
            keycloakGateway.refreshUserToken(TOKEN_DTO.getRefreshToken());
        });
    }

    @Test
    public void refreshUserToken_badRequest_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(CredentialsInvalidException.class, () -> {
            keycloakGateway.refreshUserToken(TOKEN_DTO.getRefreshToken());
        });
    }

    @Test
    public void refreshUserToken_badRequestInactiveSession_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.create(HttpStatus.BAD_REQUEST, "Session not active", new HttpHeaders(), new byte[]{}, Charset.defaultCharset()))
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(CredentialsInvalidException.class, () -> {
            keycloakGateway.refreshUserToken(TOKEN_DTO.getRefreshToken());
        });
    }

    @Test
    public void obtainUserToken_succeeds() throws AuthServiceConnectionException,
            AccountNotSetupException, CredentialsInvalidException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TOKEN_DTO));

        /* test */
        final TokenDto response = keycloakGateway.obtainUserToken(USER_1_USERNAME, USER_1_PASSWORD);
        assertNotNull(response.getAccessToken());
    }

    @Test
    public void obtainUserToken_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(AuthServiceConnectionException.class, () -> {
            keycloakGateway.obtainUserToken(USER_1_USERNAME, USER_1_PASSWORD);
        });
    }

    @Test
    public void obtainUserToken_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(CredentialsInvalidException.class, () -> {
            keycloakGateway.obtainUserToken(USER_1_USERNAME, USER_1_PASSWORD);
        });
    }

}
