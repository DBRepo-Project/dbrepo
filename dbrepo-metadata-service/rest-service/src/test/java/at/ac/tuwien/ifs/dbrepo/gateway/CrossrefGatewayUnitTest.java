package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.crossref.CrossrefDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DoiNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class CrossrefGatewayUnitTest extends BaseTest {

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
