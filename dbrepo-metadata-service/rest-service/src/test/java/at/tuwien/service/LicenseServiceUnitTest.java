package at.tuwien.service;

import at.tuwien.exception.LicenseNotFoundException;
import at.tuwien.test.AbstractUnitTest;
import at.tuwien.entities.database.License;
import at.tuwien.repository.LicenseRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class LicenseServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseService licenseService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void findAll_succeeds() {

        /* mock */
        when(licenseRepository.findAll())
                .thenReturn(List.of(LICENSE_1));

        /* test */
        final List<License> response = licenseService.findAll();
        assertEquals(1, response.size());
    }

    @Test
    public void find_succeeds() throws LicenseNotFoundException {

        /* mock */
        when(licenseRepository.findByIdentifier(LICENSE_1_IDENTIFIER))
                .thenReturn(Optional.of(LICENSE_1));

        /* test */
        final License response = licenseService.find(LICENSE_1_IDENTIFIER);
        assertEquals(LICENSE_1_IDENTIFIER, response.getIdentifier());
    }

    @Test
    public void find_fails() {

        /* mock */
        when(licenseRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(LicenseNotFoundException.class, () -> {
            licenseService.find("CC0");
        });
    }

}
