package at.tuwien.gateway;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.keycloak.TokenDto;
import at.tuwien.api.keycloak.UserDto;
import at.tuwien.exception.*;
import at.tuwien.gateway.impl.DataDbSidecarGatewayImpl;
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
@MockAmqp
@MockOpensearch
public class DataDbSidecarGatewayUnitTest extends BaseUnitTest {

    @MockBean
    @Qualifier("sidecarRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private DataDbSidecarGatewayImpl dataDbSidecarGateway;

    @Test
    public void importFile_succeeds() throws DataDbSidecarException, DataProcessingException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                        .build());

        /* test */
        dataDbSidecarGateway.importFile("data-db", 3305, "somefile.csv");
    }

    @Test
    public void importFile_response_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataProcessingException.class, () -> {
            dataDbSidecarGateway.importFile("data-db", 3305, "failed.csv");
        });
    }

    @Test
    public void importFile_unexpected_fails() {

        /* mock */
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataDbSidecarException.class, () -> {
            dataDbSidecarGateway.importFile("data-db", 3305, "failed.csv");
        });
    }

    @Test
    public void exportFile_succeeds() throws DataDbSidecarException, DataProcessingException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                        .build());

        /* test */
        dataDbSidecarGateway.exportFile("data-db", 3305, "somefile.csv");
    }

    @Test
    public void exportFile_response_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataProcessingException.class, () -> {
            dataDbSidecarGateway.exportFile("data-db", 3305, "failed.csv");
        });
    }

    @Test
    public void exportFile_unexpected_fails() {

        /* mock */
        doThrow(ResourceAccessException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataDbSidecarException.class, () -> {
            dataDbSidecarGateway.exportFile("data-db", 3305, "failed.csv");
        });
    }

}
