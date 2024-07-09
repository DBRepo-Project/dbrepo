package at.tuwien.gateway;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.exception.DoiNotFoundException;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class CrossrefGatewayUnitTest extends AbstractUnitTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private CrossrefGateway crossrefGateway;

    @Test
    public void findById_succeeds() throws DoiNotFoundException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CrossrefDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        crossrefGateway.findById("501100004729");
    }

    @Test
    public void findById_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(CrossrefDto.class));

        /* test */
        assertThrows(DoiNotFoundException.class, () -> {
            crossrefGateway.findById("501100004729");
        });
    }

}
