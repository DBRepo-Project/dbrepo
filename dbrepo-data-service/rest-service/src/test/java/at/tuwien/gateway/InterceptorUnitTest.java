package at.tuwien.gateway;

import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.ServiceException;
import at.tuwien.exception.StorageNotFoundException;
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
public class InterceptorUnitTest extends AbstractUnitTest {

    @MockBean
    @Qualifier("keycloakRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private DataDatabaseSidecarGateway dataDatabaseSidecarGateway;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void intercept_succeeds() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(TokenDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .build());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.ok()
                        .build());

        /* test */
        restTemplate.exchange("https://example.com", HttpMethod.GET, HttpEntity.EMPTY, Void.class);
    }
}
