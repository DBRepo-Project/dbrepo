package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.License;
import at.ac.tuwien.ifs.dbrepo.core.exception.LicenseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.LicenseRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class LicenseServiceUnitTest extends BaseTest {

    @MockitoBean
    private LicenseRepository licenseRepository;

    @Autowired
    private LicenseService licenseService;

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
