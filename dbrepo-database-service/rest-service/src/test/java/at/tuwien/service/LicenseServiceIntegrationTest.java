package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.License;
import at.tuwien.exception.LicenseNotFoundException;
import at.tuwien.repository.mdb.*;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class LicenseServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseService licenseService;

    @BeforeEach
    public void beforeEach() {
        licenseRepository.save(LICENSE_1);
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<License> response = licenseService.findAll();
        assertEquals(1, response.size());
    }

    @Test
    public void find_succeeds() throws LicenseNotFoundException {

        /* test */
        final License response = licenseService.find(LICENSE_1_IDENTIFIER);
        assertEquals(LICENSE_1_IDENTIFIER, response.getIdentifier());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(LicenseNotFoundException.class, () -> {
            licenseService.find("CC0");
        });
    }

}
