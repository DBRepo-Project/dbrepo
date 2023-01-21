package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.*;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.AccessEndpoint;
import at.tuwien.endpoints.LicenseEndpoint;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.mapper.AccessMapper;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.LicenseRepository;
import at.tuwien.repository.jpa.UserRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class LicenseEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private Channel channel;

    @MockBean
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseEndpoint licenseEndpoint;

    @Test
    public void list_succeeds() {

        /* mock */
        when(licenseRepository.findAll())
                .thenReturn(List.of(LICENSE_1));

        /* test */
        final ResponseEntity<List<LicenseDto>> response = licenseEndpoint.list(CONTAINER_1_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<LicenseDto> body = response.getBody();
        assertEquals(1, body.size());
        final LicenseDto license0 = body.get(0);
        assertEquals(LICENSE_1_IDENTIFIER, license0.getIdentifier());
        assertEquals(LICENSE_1_URI, license0.getUri());
    }

    @Test
    public void list_empty_succeeds() {

        /* mock */
        when(licenseRepository.findAll())
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<LicenseDto>> response = licenseEndpoint.list(CONTAINER_1_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<LicenseDto> body = response.getBody();
        assertEquals(0, body.size());
    }

}
