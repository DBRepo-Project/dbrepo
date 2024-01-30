package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.entities.database.License;
import at.tuwien.exception.LicenseNotFoundException;
import at.tuwien.repository.mdb.LicenseRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class LicenseServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseService licenseService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

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
