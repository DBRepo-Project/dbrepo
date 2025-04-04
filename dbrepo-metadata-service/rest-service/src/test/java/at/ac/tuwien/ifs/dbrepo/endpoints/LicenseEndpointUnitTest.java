package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.LicenseDto;
import at.ac.tuwien.ifs.dbrepo.repository.LicenseRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class LicenseEndpointUnitTest extends BaseTest {

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
        final ResponseEntity<List<LicenseDto>> response = licenseEndpoint.list();
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
        final ResponseEntity<List<LicenseDto>> response = licenseEndpoint.list();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<LicenseDto> body = response.getBody();
        assertEquals(0, body.size());
    }

}
