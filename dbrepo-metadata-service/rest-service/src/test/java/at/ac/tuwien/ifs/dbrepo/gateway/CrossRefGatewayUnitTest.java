package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.crossref.CrossRefDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DoiNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
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
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class CrossRefGatewayUnitTest extends BaseTest {

    @MockBean
    @Qualifier("crossRefServiceRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private CrossRefGateway crossRefGateway;

    @Test
    public void findById_succeeds() throws DoiNotFoundException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(CrossRefDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        crossRefGateway.findById("501100004729");
    }

    @Test
    public void findById_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(CrossRefDto.class));

        /* test */
        assertThrows(DoiNotFoundException.class, () -> {
            crossRefGateway.findById("501100004729");
        });
    }

}
