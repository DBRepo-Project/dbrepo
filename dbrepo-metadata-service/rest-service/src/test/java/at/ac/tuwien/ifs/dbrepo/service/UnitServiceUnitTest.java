package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnUnit;
import at.ac.tuwien.ifs.dbrepo.core.exception.UnitNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.UnitRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UnitServiceUnitTest extends BaseTest {

    @MockBean
    private UnitRepository unitRepository;

    @Autowired
    private UnitService unitService;

    @Test
    @Transactional
    public void findAll_succeeds() {

        /* mock */
        when(unitRepository.findAll())
                .thenReturn(List.of(UNIT_1));

        /* test */
        final List<TableColumnUnit> response = unitService.findAll();
        assertEquals(1, response.size());
        assertTrue(response.stream().anyMatch(c -> c.getUri().equals(UNIT_1_URI)));
    }

    @Test
    @Transactional
    public void find_succeeds() throws UnitNotFoundException {

        /* mock */
        when(unitRepository.findByUri(UNIT_1_URI))
                .thenReturn(Optional.of(UNIT_1));

        /* test */
        final TableColumnUnit response = unitService.find(UNIT_1_URI);
        assertEquals(UNIT_1_URI, response.getUri());
        assertEquals(UNIT_1_NAME, response.getName());
        assertEquals(UNIT_1_DESCRIPTION, response.getDescription());
    }

    @Test
    @Transactional
    public void findUnit_fails() {

        /* mock */
        when(unitRepository.findByUri(anyString()))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(UnitNotFoundException.class, () -> {
            unitService.find("http://example.com/rdf");
        });
    }

}
