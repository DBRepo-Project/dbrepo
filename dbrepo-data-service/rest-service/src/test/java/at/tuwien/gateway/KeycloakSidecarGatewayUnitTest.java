package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.ServiceConnectionException;
import at.tuwien.exception.ServiceException;
import at.tuwien.test.AbstractUnitTest;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class KeycloakSidecarGatewayUnitTest extends AbstractUnitTest {

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private KeycloakGateway keycloakGateway;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void obtainUserToken_succeeds() throws ServiceException, RemoteUnavailableException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .build());

        /* test */
        final TokenDto response = keycloakGateway.obtainUserToken(USER_1_USERNAME, USER_1_PASSWORD);
    }

    @Test
    public void obtainUserToken_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            keycloakGateway.obtainUserToken(USER_1_USERNAME, USER_1_PASSWORD);
        });
    }

    @Test
    public void obtainUserToken_badRequest_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class));

        /* test */
        assertThrows(ServiceException.class, () -> {
            keycloakGateway.obtainUserToken(USER_1_USERNAME, USER_1_PASSWORD);
        });
    }

    @Test
    public void obtainUserToken_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(ServiceException.class, () -> {
            keycloakGateway.obtainUserToken(USER_1_USERNAME, USER_1_PASSWORD);
        });
    }

}
