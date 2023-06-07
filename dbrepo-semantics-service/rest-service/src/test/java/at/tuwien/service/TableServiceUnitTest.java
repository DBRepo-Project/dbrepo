package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.repository.sdb.*;
import at.tuwien.repository.sdb.TableColumnIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceUnitTest extends BaseUnitTest {

    @MockBean
    private UnitIdxRepository unitIdxRepository;

    @MockBean
    private ConceptIdxRepository conceptIdxRepository;

    @MockBean
    private TableIdxRepository tableIdxRepository;

    @MockBean
    private TableColumnIdxRepository tableColumnIdxRepository;

    @MockBean
    private TableRepository tableRepository;

    @Autowired
    private TableService tableService;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @Test
    public void find_success() throws TableNotFoundException {

        /* mock */
        when(tableRepository.findByDatabaseIdAndId(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final Table response = tableService.find(DATABASE_1_ID, TABLE_1_ID);
        assertEquals(TABLE_1_ID, response.getId());
    }

    @Test
    public void find_fails() {

        /* mock */
        when(tableRepository.findByDatabaseIdAndId(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.find(DATABASE_1_ID, TABLE_1_ID);
        });
    }
}
