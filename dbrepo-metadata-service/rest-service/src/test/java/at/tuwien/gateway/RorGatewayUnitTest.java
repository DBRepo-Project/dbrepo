package at.tuwien.gateway;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.ror.RorDto;
import at.tuwien.exception.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class RorGatewayUnitTest extends AbstractUnitTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private RorGateway rorGateway;

    @Test
    public void findById_succeeds() throws RorNotFoundException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(RorDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        rorGateway.findById("04d836q62");
    }

    @Test
    public void findById_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(RorDto.class));

        /* test */
        assertThrows(RorNotFoundException.class, () -> {
            rorGateway.findById("04d836q62");
        });
    }

}
